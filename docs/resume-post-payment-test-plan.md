# Piano di test - Resume post payment

## 1. Scopo

Questo documento definisce la batteria di test condivisibile con QA, sviluppo e Operations per verificare il recupero delle notifiche pagate che non hanno proseguito il workflow di invio cartaceo.

Il piano copre:

- lo script `pn-resume-post-payment` che valida i CSV e pubblica i messaggi su SQS;
- il contratto della coda e il consumer di `pn-delivery-push-workflow`;
- le regole comuni e specifiche dei tre tipi di ripresa;
- idempotenza, concorrenza, retry, DLQ e allarmi;
- la regressione dei flussi ordinari PREPARE e SEND richiesta da US-1;
- scenari end-to-end dallo script fino a Paper Channel e Timeline.

Le prove sono pensate per un ambiente DEV/QA reale. LocalStack e MockServer non fanno parte delle precondizioni.

## 2. Contratto atteso e gate di conformita

### 2.1 Contratto funzionale

I valori ammessi per `resumeType` sono:

- `FIRST_ATTEMPT`;
- `SECOND_ATTEMPT`;
- `SIMPLE_REGISTERED_LETTER`.

Il messaggio SQS atteso e:

```json
{
  "iun": "<IUN>",
  "recIndex": 0,
  "resumeType": "FIRST_ATTEMPT"
}
```

Gli scarti funzionali devono essere registrati e confermati alla coda (ACK). Gli errori tecnici devono propagarsi, rendendo il messaggio nuovamente elaborabile fino al redrive in DLQ.

### 2.2 Gate bloccanti noti

| Gate | Comportamento richiesto dall'SRS | Stato da verificare prima della campagna | Criterio di superamento |
| --- | --- | --- | --- |
| GATE-01 | Lo script accetta esattamente un argomento posizionale e legge la configurazione AWS esclusivamente da `AWS_PROFILE`, `AWS_REGION`/`AWS_DEFAULT_REGION`, `PN_RESUME_POST_PAYMENT_QUEUE_URL`, `SQS_ENDPOINT_URL`. Le opzioni CLI di configurazione sono vietate. | L'implementazione osservata usa `--profile`, `--region`, `--queue-url`, `--endpoint` e non il contratto env-only. | Tutti i test `SCR-CFG-*` passano secondo l'SRS. Fino ad allora il difetto e bloccante. |
| GATE-02 | Un pagamento non deve bloccare PREPARE o SEND; visualizzazione e cancellazione devono continuare a bloccarli. | L'implementazione osservata di `PaperChannelServiceImpl` usa ancora un controllo `viewed or paid`. | Tutti i test `REG-US1-*` passano sul build distribuito. Fino ad allora il difetto e bloccante. |
| GATE-03 | Il trattamento economico delle raccomandate semplici gia perfezionate deve essere deciso dal business. | Decisione non inclusa nei criteri tecnici disponibili. | Decisione formalizzata e casi `E2E-SRL-02/03` aggiornati con l'esito atteso definitivo. |

Un risultato coerente con l'implementazione corrente ma contrario a questa sezione deve essere registrato come test KO, non come variazione accettata.

## 3. Ambiente, accessi e osservabilita

### 3.1 Prerequisiti generali

Prima della campagna devono essere disponibili:

- build sotto test e commit/tag annotati nel verbale;
- ambiente DEV/QA con `pn-delivery-push-workflow` attivo;
- URL e ARN della coda resume e della relativa DLQ;
- credenziali con permesso di invio alla sola coda dell'ambiente;
- accesso in lettura ai log applicativi, Timeline, metriche SQS e allarmi;
- accesso alle evidenze di Paper Channel o supporto del relativo team;
- strumenti approvati per creare o individuare notifiche nei precisi stati richiesti;
- finestra concordata con Operations per test di indisponibilita, retry, DLQ e allarmi;
- correlazione temporale affidabile tra i sistemi e timezone annotata.

Non modificare direttamente dati persistiti per costruire le precondizioni, salvo procedura QA approvata. Preferire notifiche prodotte dai normali flussi o fixture gestite dai team proprietari.

### 3.2 Evidenze minime da raccogliere

Per ogni caso conservare:

- ID test, data/ora, ambiente, build e operatore;
- CSV e comando usato, oscurando dati personali e credenziali;
- IUN, `recIndex`, `resumeType` e SQS `MessageId` se disponibile;
- estratto Timeline prima e dopo il test;
- log strutturati dello script e dell'applicazione;
- evidenza della chiamata a Paper Channel o della sua assenza;
- contatori della coda/DLQ prima e dopo, quando pertinenti;
- risultato PASS/FAIL/BLOCKED e riferimento al difetto.

Marker applicativi attesi:

- processo: `RESUME_POST_PAYMENT_CONSUMER`;
- successo: `RESUME_POST_PAYMENT_SUCCESS`;
- scarto/errore funzionale: `RESUME_POST_PAYMENT_ERROR`.

Marker script attesi:

- `RESUME_POST_PAYMENT_MALFORMED_ROW`;
- `RESUME_POST_PAYMENT_PUBLISHED`;
- `RESUME_POST_PAYMENT_PUBLICATION_FAILED`;
- `RESUME_POST_PAYMENT_SUMMARY`;
- `RESUME_POST_PAYMENT_SCRIPT_ERROR`.

### 3.3 Convenzioni sui dati

Usare IUN dedicati e riconoscibili, senza dati personali reali. Per i test negativi variare una sola precondizione rispetto al baseline; tutte le condizioni non citate devono restare valide.

Baseline comuni:

- **BASE-COMMON**: notifica esistente; IUN coerente; destinatario `recIndex` esistente; evento `PAYMENT` per lo stesso destinatario; nessuna cancellazione; nessuna visualizzazione; nessun `NOTIFICATION_TIMELINE_REWORKED`.
- **BASE-FIRST**: `BASE-COMMON`; `SCHEDULE_ANALOG_WORKFLOW` dello stesso destinatario con `schedulingDate` precedente o uguale a `now - 10h`; nessun `PREPARE_ANALOG_DOMICILE` con `sentAttemptMade=0`.
- **BASE-SECOND-KO**: `BASE-COMMON`; `SEND_ANALOG_DOMICILE` con tentativo 0; feedback analogico KO relativo al tentativo 0; nessun `PREPARE_ANALOG_DOMICILE` con tentativo 1.
- **BASE-SECOND-TIMEOUT**: come `BASE-SECOND-KO`, sostituendo il KO con `SEND_ANALOG_TIMEOUT` del tentativo 0.
- **BASE-SIMPLE**: `BASE-COMMON`; `DIGITAL_FAILURE_WORKFLOW`; `SCHEDULE_REFINEMENT`; indirizzo fisico valorizzato; nessun `PREPARE_SIMPLE_REGISTERED_LETTER`.

Gli elementi Timeline devono riferirsi allo stesso `recIndex` del messaggio. Elementi di un altro destinatario non soddisfano una precondizione.

## 4. Criteri di ingresso e uscita

### Ingresso

- I gate e le decisioni ancora aperte sono noti e assegnati;
- i dati baseline sono stati verificati immediatamente prima della prova;
- code e servizi non presentano backlog o allarmi estranei alla campagna;
- e disponibile una procedura di pulizia o isolamento dei messaggi di test.

### Uscita

- tutti i test P0 e P1 applicabili sono eseguiti;
- nessun difetto bloccante o critico e aperto;
- ogni requisito e coperto dalla matrice di tracciabilita;
- retry e redrive sono provati almeno una volta in ambiente controllato;
- per ogni tipo di resume esiste almeno un E2E positivo con evidenza Timeline e Paper Channel;
- non sono presenti duplicazioni di PREPARE o SEND attribuibili alla campagna.

## 5. Test dello script

Eseguire questi test dal repository `pn-troubleshooting`, usando la versione dello script candidata al rilascio. Per i test che non devono pubblicare, verificare anche che i contatori SQS non cambino.

### 5.1 Invocazione e configurazione AWS

| ID | Pri | Precondizioni | Azione | Risultato atteso ed evidenze |
| --- | --- | --- | --- | --- |
| SCR-ARG-01 | P0 | Script installato; file `csv/FIRST_ATTEMPT.csv` valido; variabili AWS valide. | Eseguire con il solo argomento `FIRST_ATTEMPT`. | Il file associato viene letto e la lavorazione parte. Nessun errore di invocazione. |
| SCR-ARG-02 | P0 | Script installato; nessuna pubblicazione preesistente correlata. | Eseguire senza argomento posizionale. | Exit code 1; `RESUME_POST_PAYMENT_SCRIPT_ERROR`; nessuna lettura/pubblicazione utile. |
| SCR-ARG-03 | P0 | Come `SCR-ARG-02`. | Eseguire con due tipi posizionali. | Exit code 1 per numero argomenti non valido; nessun messaggio pubblicato. |
| SCR-ARG-04 | P0 | Come `SCR-ARG-02`. | Eseguire con `UNKNOWN_TYPE`. | Exit code 1 per tipo non supportato; nessun messaggio pubblicato. |
| SCR-MAP-01 | P1 | Tre CSV validi e distinti, uno per ciascun tipo. | Eseguire separatamente i tre tipi. | Ogni esecuzione legge esattamente `csv/<resumeType>.csv`; i payload contengono il tipo richiesto. |
| SCR-CFG-01 | P0 | `AWS_PROFILE` autenticato; `AWS_REGION` valido; `PN_RESUME_POST_PAYMENT_QUEUE_URL` valido; CSV valido. | Eseguire con il solo `resumeType`. | Configurazione letta dall'ambiente; pubblicazione riuscita; exit code 0. |
| SCR-CFG-02 | P0 | `AWS_PROFILE` assente; default credential provider chain valida; regione e URL in env. | Eseguire con il solo `resumeType`. | Credenziali risolte dalla chain standard; pubblicazione riuscita. |
| SCR-CFG-03 | P0 | `AWS_REGION` assente; `AWS_DEFAULT_REGION` valido; altre env valide. | Eseguire. | Viene usata `AWS_DEFAULT_REGION`; pubblicazione riuscita. |
| SCR-CFG-04 | P0 | Entrambe le regioni assenti; altre env valide. | Eseguire. | Exit code 1; errore di regione mancante; nessuna pubblicazione. |
| SCR-CFG-05 | P1 | Regione env con formato non valido; altre env valide. | Eseguire. | Exit code 1; errore di regione; nessuna pubblicazione. |
| SCR-CFG-06 | P0 | URL coda assente; altre env valide. | Eseguire. | Exit code 1; errore di URL mancante; nessuna pubblicazione. |
| SCR-CFG-07 | P1 | URL coda malformato o relativo a una risorsa non consentita. | Eseguire. | Validazione locale o errore AWS esplicito; exit code 1; nessun falso `PUBLISHED`. |
| SCR-CFG-08 | P1 | `SQS_ENDPOINT_URL` assente; configurazione AWS reale valida. | Eseguire. | Viene usato l'endpoint AWS standard della regione. |
| SCR-CFG-09 | P1 | Endpoint alternativo approvato in un ambiente di test; altre env valide. | Impostare `SQS_ENDPOINT_URL` ed eseguire. | Il client usa l'endpoint configurato. Non eseguire contro endpoint non autorizzati. |
| SCR-CFG-10 | P0 | Env-only valida; CSV valido. | Aggiungere una qualsiasi opzione `--region`, `--queue-url`, `--profile` o `--endpoint`. | Exit code 1 per opzione vietata/sconosciuta; nessuna pubblicazione. Questo e il test di conformita GATE-01. |
| SCR-CFG-11 | P1 | Profilo indicato ma sessione SSO scaduta; CSV valido. | Eseguire. | Errore credenziali strutturato; exit code 1; nessuna credenziale nei log. |

### 5.2 File e formato CSV

| ID | Pri | Precondizioni | Azione | Risultato atteso ed evidenze |
| --- | --- | --- | --- | --- |
| SCR-CSV-01 | P0 | File atteso assente. | Eseguire il tipo corrispondente. | Exit code 1; errore file non esistente; nessuna chiamata SQS. |
| SCR-CSV-02 | P1 | File atteso presente ma non leggibile dall'utente di esecuzione. | Eseguire. | Exit code 1; errore leggibilita; nessuna chiamata SQS. Ripristinare i permessi. |
| SCR-CSV-03 | P0 | CSV con header esatto `iun,recIndex` e una riga valida. | Eseguire. | Una pubblicazione; `recIndex` JSON numerico; exit code 0. |
| SCR-CSV-04 | P0 | CSV con colonne invertite. | Eseguire. | File rifiutato prima delle pubblicazioni; exit code 1. |
| SCR-CSV-05 | P1 | CSV con maiuscole diverse, spazi nell'header o colonna aggiuntiva. | Eseguire una variante alla volta. | Ogni variante e rifiutata; nessuna pubblicazione. |
| SCR-CSV-06 | P1 | CSV con BOM UTF-8 prima dell'header esatto. | Eseguire. | Header accettato; righe valide elaborate normalmente. |
| SCR-CSV-07 | P1 | CSV con righe vuote e whitespace tra righe valide. | Eseguire. | Righe vuote ignorate e non incluse in `totalRows`; valide pubblicate. |
| SCR-CSV-08 | P1 | CSV con valori correttamente quotati, inclusi delimitatori/whitespace gestibili dal parser. | Eseguire. | Parsing conforme al CSV; normalizzazione senza corrompere i campi. Se il valore normalizzato e invalido, la riga e marcata malformed. |
| SCR-CSV-09 | P0 | CSV vuoto o contenente solo header/righe vuote. | Eseguire. | Nessuna pubblicazione; summary coerente con zero record; exit code secondo SRS, atteso 0 in assenza di errori. |
| SCR-CSV-10 | P1 | CSV molto grande entro il volume operativo concordato. | Eseguire monitorando durata e memoria. | Tutto il file e validato prima della prima pubblicazione; nessun crash; contatori corretti. |

### 5.3 Validazione, normalizzazione e deduplica

| ID | Pri | Precondizioni | Azione | Risultato atteso ed evidenze |
| --- | --- | --- | --- | --- |
| SCR-ROW-01 | P0 | CSV con IUN valido circondato da spazi e `recIndex=0`. | Eseguire. | IUN pubblicato senza spazi; `recIndex` pubblicato come numero 0. |
| SCR-ROW-02 | P0 | Una riga con IUN vuoto e una riga valida. | Eseguire. | Riga vuota: `MALFORMED_ROW`; riga valida pubblicata; esecuzione non interrotta. |
| SCR-ROW-03 | P0 | Varianti separate con `recIndex` assente, testo, decimale e negativo; una riga valida di controllo. | Eseguire ogni variante. | Ogni variante invalida e scartata con linea/codice; controllo pubblicato. |
| SCR-ROW-04 | P1 | `recIndex` oltre il safe integer JavaScript; una riga valida di controllo. | Eseguire. | Valore unsafe scartato; nessun arrotondamento/pubblicazione errata. |
| SCR-ROW-05 | P1 | Riga con numero colonne minore e riga con numero colonne maggiore. | Eseguire. | Entrambe malformed; nessun payload parziale. |
| SCR-ROW-06 | P0 | Stessa coppia `(iun, recIndex)` ripetuta, anche con whitespace equivalente. | Eseguire. | Prima occorrenza pubblicata una volta; successive contate in `duplicateRows`. |
| SCR-ROW-07 | P0 | Stesso IUN con due `recIndex` diversi. | Eseguire. | Entrambe le coppie pubblicate; nessuna falsa deduplica. |
| SCR-ROW-08 | P1 | CSV misto con valide, malformed, duplicate e righe vuote; conteggi attesi calcolati prima. | Eseguire. | `totalRows = validRows + malformedRows`; `publishableRecords = validRows - duplicateRows`; ogni unica valida e pubblicata. |
| SCR-ROW-09 | P0 | CSV misto come sopra; accesso ai log dei servizi Delivery/Timeline e dell'applicazione. | Eseguire lo script con consumer temporaneamente fermo o correlare la sola finestra di pubblicazione. | Lo script chiama esclusivamente SQS: nessuna chiamata diretta a Delivery, Timeline o API di business. |

### 5.4 Pubblicazione, log ed exit code

| ID | Pri | Precondizioni | Azione | Risultato atteso ed evidenze |
| --- | --- | --- | --- | --- |
| SCR-PUB-01 | P0 | CSV con N record unici validi; SQS disponibile. | Eseguire. | N invii sequenziali; N `PUBLISHED` con `MessageId`; summary con `publishedMessages=N`, `failedPublications=0`; exit 0. |
| SCR-PUB-02 | P1 | SQS risponde senza `MessageId`, inducibile tramite harness approvato o test automatizzato; due record. | Eseguire. | Prima risposta trattata come fallimento; `PUBLICATION_FAILED`; elaborazione del record successivo; exit 1. |
| SCR-PUB-03 | P0 | Tre record; errore SQS tecnico controllato sul secondo, servizio sano sul terzo. | Eseguire. | Il terzo viene tentato; 2 successi, 1 fallimento; summary coerente; exit 1. |
| SCR-PUB-04 | P1 | Tutti i publish falliscono per autorizzazione negata. | Eseguire. | Un errore per record, nessuna interruzione anticipata non prevista; exit 1; nessun segreto nei log. |
| SCR-PUB-05 | P0 | CSV con sole malformed/duplicate ma nessun errore preliminare o SQS. | Eseguire. | Summary completo; malformed e duplicate da soli non causano exit non-zero; exit 0. |
| SCR-PUB-06 | P1 | Interruzione inattesa controllata prima della pubblicazione. | Eseguire. | `SCRIPT_ERROR`; exit 1; assenza di summary ingannevole di successo. |
| SCR-PUB-07 | P1 | CSV con conteggi noti e un publish fallito. | Eseguire. | `publishableRecords = publishedMessages + failedPublications`; tutti i marker sono JSON/strutturati e ricercabili. |

## 6. Contratto SQS e validazione sintattica del consumer

Pubblicare direttamente in coda per isolare il consumer dallo script. In tutti i casi di scarto sintattico il messaggio deve essere ACKato, non deve raggiungere il business handler e non deve comparire in DLQ.

| ID | Pri | Precondizioni | Messaggio/azione | Risultato atteso ed evidenze |
| --- | --- | --- | --- | --- |
| CON-SYN-01 | P0 | Consumer attivo; IUN `BASE-FIRST`. | JSON valido completo. | Il consumer aggiunge IUN/recIndex al contesto e delega al handler. |
| CON-SYN-02 | P0 | Consumer attivo; metriche coda annotate. | Testo non JSON. | `RESUME_POST_PAYMENT_ERROR` con motivo parse; ACK; nessuna chiamata Delivery/Timeline/Paper. |
| CON-SYN-03 | P1 | Come sopra. | Payload JSON `null`. | Errore `null event`; ACK; nessuna elaborazione business. |
| CON-SYN-04 | P0 | Come sopra. | Oggetto senza `iun`, con IUN `null`, vuoto e blank in esecuzioni separate. | Errore `iun is required`; ACK; nessuna chiamata downstream. |
| CON-SYN-05 | P0 | Come sopra. | Oggetto senza `recIndex` o con `null`. | Errore `recIndex is required`; ACK. |
| CON-SYN-06 | P0 | Come sopra. | `recIndex=-1`. | Errore `recIndex must be non-negative`; ACK. |
| CON-SYN-07 | P0 | Come sopra. | Oggetto senza `resumeType` o con `null`. | Errore `resumeType is required`; ACK. |
| CON-SYN-08 | P0 | Come sopra. | `resumeType="UNSUPPORTED"`. | Errore di deserializzazione enum; ACK; nessuna chiamata downstream. |
| CON-SYN-09 | P1 | Come sopra. | Tipi JSON errati, per esempio `recIndex="abc"`, array o oggetto annidato. | Errore di deserializzazione; ACK; nessun retry tecnico. |
| CON-SYN-10 | P1 | Come sopra. | JSON con campi aggiuntivi. | Comportamento conforme alla configurazione Jackson documentata: accettazione se gli unknown sono consentiti, altrimenti scarto sintattico con ACK. L'esito deve essere stabile e formalizzato. |

## 7. Regole comuni di eleggibilita

Usare un messaggio sintatticamente valido. Gli scarti funzionali devono produrre `RESUME_POST_PAYMENT_ERROR`, il reason code indicato, ACK e zero chiamate a Paper Channel.

| ID | Pri | Precondizioni specifiche | Azione | Risultato atteso |
| --- | --- | --- | --- | --- |
| APP-COM-01 | P0 | `BASE-FIRST`. | Inviare `FIRST_ATTEMPT`. | Regole comuni superate; il caso prosegue nelle regole FIRST. |
| APP-COM-02 | P0 | IUN inesistente o non recuperabile funzionalmente. | Inviare un evento valido. | Scarto funzionale coerente con contratto del servizio; nessuna Paper. Se l'assenza e restituita come errore tecnico, classificare e documentare il retry. |
| APP-COM-03 | P0 | Notifica caricata con IUN diverso da quello richiesto, predisposta con fixture controllata. | Inviare. | `NOTIFICATION_IUN_MISMATCH`; ACK. |
| APP-COM-04 | P0 | Notifica esistente ma `recIndex` fuori intervallo. | Inviare. | `RECIPIENT_NOT_FOUND`; ACK; Timeline non necessaria. |
| APP-COM-05 | P0 | Baseline del tipo, ma senza `PAYMENT` per il destinatario. | Inviare. | `PAYMENT_NOT_FOUND`; ACK. |
| APP-COM-06 | P1 | `PAYMENT` presente solo per un altro `recIndex`. | Inviare. | `PAYMENT_NOT_FOUND` per il destinatario richiesto; ACK. |
| APP-COM-07 | P0 | Baseline del tipo con cancellazione richiesta. | Inviare. | `NOTIFICATION_CANCELLED`; ACK. |
| APP-COM-08 | P0 | Baseline del tipo con visualizzazione del destinatario. | Inviare. | `NOTIFICATION_VIEWED`; ACK. |
| APP-COM-09 | P0 | Baseline del tipo con `NOTIFICATION_TIMELINE_REWORKED` per il destinatario. | Inviare. | `NOTIFICATION_REWORKED`; ACK. |
| APP-COM-10 | P1 | Reworked/viewed presenti solo per altro destinatario; destinatario richiesto conforme. | Inviare. | Nessun falso scarto comune; prosegue nelle regole specifiche. |
| APP-COM-11 | P1 | Due condizioni invalide contemporanee, con dati controllati. | Inviare. | Reason code deterministico secondo l'ordine di validazione; nessuna Paper. Annotare il reason per prevenire regressioni. |

## 8. FIRST_ATTEMPT

| ID | Pri | Precondizioni specifiche | Azione | Risultato atteso ed evidenze |
| --- | --- | --- | --- | --- |
| APP-FST-01 | P0 | `BASE-FIRST`; `schedulingDate < now-10h`. | Inviare `FIRST_ATTEMPT`. | Chiamata al percorso nativo analogico con tentativo 0; `RESUME_POST_PAYMENT_SUCCESS`; operation `PREPARE_ANALOG_NOTIFICATION_FIRST_ATTEMPT`; nuovo PREPARE coerente. |
| APP-FST-02 | P0 | `BASE-COMMON`; assente `SCHEDULE_ANALOG_WORKFLOW`. | Inviare. | `SCHEDULE_ANALOG_WORKFLOW_NOT_FOUND`; ACK; nessuna Paper. |
| APP-FST-03 | P0 | `BASE-COMMON`; schedule con data maggiore di `now-10h`. | Inviare. | `SCHEDULE_ANALOG_WORKFLOW_TOO_RECENT`; ACK. |
| APP-FST-04 | P1 | `BASE-COMMON`; schedule esattamente a `now-10h`, con tolleranza clock concordata. | Inviare. | Eleggibile: il limite e inclusivo (`<=`). |
| APP-FST-05 | P1 | `BASE-COMMON`; schedule con data nulla o non utilizzabile, ottenuto da fixture controllata. | Inviare. | Scarto `SCHEDULE_ANALOG_WORKFLOW_TOO_RECENT`; nessuna eccezione non gestita. |
| APP-FST-06 | P0 | `BASE-FIRST` ma PREPARE tentativo 0 gia presente. | Inviare. | `PREPARE_ALREADY_PRESENT`; ACK; nessun secondo PREPARE. |
| APP-FST-07 | P1 | `BASE-FIRST`; PREPARE presente solo per tentativo 1 o altro destinatario. | Inviare. | Nessun falso duplicato per tentativo 0; caso eleggibile. |

## 9. SECOND_ATTEMPT

| ID | Pri | Precondizioni specifiche | Azione | Risultato atteso ed evidenze |
| --- | --- | --- | --- | --- |
| APP-SND-01 | P0 | `BASE-SECOND-KO`. | Inviare `SECOND_ATTEMPT`. | Percorso analogico con tentativo 1; success marker; operation `PREPARE_ANALOG_NOTIFICATION_SECOND_ATTEMPT`; nuovo PREPARE tentativo 1. |
| APP-SND-02 | P0 | `BASE-SECOND-TIMEOUT`. | Inviare. | Stesso esito positivo di `APP-SND-01`. |
| APP-SND-03 | P0 | `BASE-COMMON`; trigger KO/timeout presente ma assente `SEND_ANALOG_DOMICILE` tentativo 0. | Inviare. | `FIRST_ANALOG_SEND_NOT_FOUND`; ACK; nessuna Paper. |
| APP-SND-04 | P0 | `BASE-COMMON`; SEND tentativo 0 presente, ma assenti sia KO sia timeout. | Inviare. | `SECOND_ATTEMPT_TRIGGER_NOT_FOUND`; ACK. |
| APP-SND-05 | P1 | `BASE-COMMON`; SEND tentativo 0 e feedback positivo, senza KO/timeout. | Inviare. | `SECOND_ATTEMPT_TRIGGER_NOT_FOUND`; nessuna ripresa indebita. |
| APP-SND-06 | P1 | `BASE-COMMON`; SEND tentativo 0, KO e timeout entrambi presenti. | Inviare. | Eleggibile una sola volta; un solo PREPARE tentativo 1. |
| APP-SND-07 | P0 | `BASE-SECOND-KO` ma PREPARE tentativo 1 gia presente. | Inviare. | `PREPARE_ALREADY_PRESENT`; ACK; nessun duplicato. |
| APP-SND-08 | P1 | SEND/trigger validi presenti solo per altro destinatario o altro tentativo. | Inviare. | Reason coerente con l'elemento mancante per il destinatario richiesto; nessuna Paper. |

## 10. SIMPLE_REGISTERED_LETTER

| ID | Pri | Precondizioni specifiche | Azione | Risultato atteso ed evidenze |
| --- | --- | --- | --- | --- |
| APP-SRL-01 | P0 | `BASE-SIMPLE`. | Inviare `SIMPLE_REGISTERED_LETTER`. | Percorso nativo raccomandata semplice; `RESUME_POST_PAYMENT_SUCCESS`; operation `PREPARE_SIMPLE_REGISTERED_LETTER`; nuovo PREPARE coerente. |
| APP-SRL-02 | P0 | `BASE-COMMON`; schedule refinement e indirizzo presenti, ma assente `DIGITAL_FAILURE_WORKFLOW`. | Inviare. | `DIGITAL_FAILURE_WORKFLOW_NOT_FOUND`; ACK. |
| APP-SRL-03 | P0 | `BASE-COMMON`; digital failure e indirizzo presenti, ma assente `SCHEDULE_REFINEMENT`. | Inviare. | `SCHEDULE_REFINEMENT_NOT_FOUND`; ACK. |
| APP-SRL-04 | P0 | `BASE-COMMON`; digital failure e schedule presenti, ma indirizzo fisico assente. | Inviare. | `PHYSICAL_ADDRESS_NOT_FOUND`; ACK. |
| APP-SRL-05 | P0 | `BASE-SIMPLE` ma PREPARE semplice gia presente. | Inviare. | `PREPARE_ALREADY_PRESENT`; ACK; nessun secondo PREPARE. |
| APP-SRL-06 | P1 | Elementi richiesti presenti solo per altro destinatario. | Inviare. | Scarto con il primo reason specifico mancante per il destinatario richiesto. |

## 11. Idempotenza e concorrenza

| ID | Pri | Precondizioni | Azione | Risultato atteso ed evidenze |
| --- | --- | --- | --- | --- |
| APP-IDM-01 | P0 | Un baseline eleggibile per ciascun tipo. | Elaborare con successo, poi reinviare lo stesso messaggio. | Prima esecuzione: successo; seconda: `PREPARE_ALREADY_PRESENT`; un solo PREPARE/Paper prepare complessivo. |
| APP-IDM-02 | P0 | `BASE-FIRST`; due copie dello stesso messaggio pronte quasi simultaneamente. | Pubblicarle in rapida successione o in concorrenza controllata. | Nessuna duplicazione business. Se entrambe superano il controllo e creano due PREPARE, aprire difetto di race condition. |
| APP-IDM-03 | P0 | `BASE-SECOND-KO`; concorrenza controllata. | Come sopra per SECOND. | Un solo PREPARE tentativo 1 e una sola richiesta Paper. |
| APP-IDM-04 | P0 | `BASE-SIMPLE`; concorrenza controllata. | Come sopra per SIMPLE. | Un solo PREPARE semplice e una sola richiesta Paper. |
| APP-IDM-05 | P1 | Stesso IUN con due destinatari entrambi eleggibili. | Inviare i due `recIndex`. | Lavorazioni indipendenti; nessuna deduplica cross-recipient. |
| APP-IDM-06 | P1 | Stesso IUN/recIndex con due `resumeType`, ma solo uno coerente con la Timeline. | Inviare entrambi. | Solo il tipo coerente puo proseguire; l'altro viene scartato con reason specifico. |
| APP-IDM-07 | P0 | PREPARE presente ma SEND assente, cioe caso esplicitamente escluso dal resume automatico. | Inviare il resume corrispondente. | `PREPARE_ALREADY_PRESENT`; nessun re-prepare. Il recupero PREPARE-to-SEND segue una procedura operativa separata. |

## 12. Errori tecnici, retry, DLQ e allarmi

Questi test richiedono una finestra approvata. La configurazione attesa della coda e `VisibilityTimeout=300s` e `MaxReceiveCount=10`; verificare i valori effettivamente distribuiti prima di eseguire.

| ID | Pri | Precondizioni | Guasto/azione | Risultato atteso ed evidenze |
| --- | --- | --- | --- | --- |
| OPS-RET-01 | P0 | Messaggio eleggibile; Delivery reso temporaneamente indisponibile con fault injection approvata. | Inviare e mantenere il guasto per un tentativo. | Eccezione tecnica; nessun ACK; messaggio invisibile durante il visibility timeout e poi ritentato; nessun falso error funzionale. |
| OPS-RET-02 | P0 | Come sopra, Timeline read indisponibile. | Inviare. | Retry, non ACK; nessuna Paper. |
| OPS-RET-03 | P1 | FIRST/SECOND eleggibile; servizio AAR/allegati necessario al percorso nativo indisponibile. | Inviare. | Errore tecnico propagato; retry; nessun successo prematuro. |
| OPS-RET-04 | P0 | Baseline eleggibile; Paper prepare risponde con errore tecnico. | Inviare. | Retry; `RESUME_POST_PAYMENT_SUCCESS` assente; consistenza Timeline verificata prima del retry. |
| OPS-RET-05 | P0 | Paper prepare completato ma inserimento Timeline fallisce in modo controllato. | Inviare. | Retry senza ACK. Verificare con particolare attenzione che il recupero non duplichi la richiesta Paper; un duplicato e difetto critico. |
| OPS-RET-06 | P0 | Uno dei guasti precedenti attivo per il primo tentativo e poi rimosso; dato ancora eleggibile/idempotente. | Ripristinare il servizio prima del retry. | Retry automatico riuscito; un solo esito business finale; coda svuotata; nessun DLQ. |
| OPS-RET-07 | P0 | Messaggio con errore tecnico permanente isolato; redrive e allarmi verificati; approvazione Operations. | Lasciare fallire oltre `MaxReceiveCount`. | Circa 10 ricezioni secondo configurazione, poi messaggio in DLQ; payload e attributi diagnosticabili; nessun ACK anticipato. |
| OPS-RET-08 | P0 | DLQ vuota e allarme collegato al canale operativo. | Eseguire `OPS-RET-07`. | Allarme DLQ passa in ALARM e notifica il canale previsto; evidenza ARN/timestamp. |
| OPS-RET-09 | P1 | Coda isolata; soglia age alarm nota; consumer sospeso con approvazione. | Pubblicare un messaggio e superare la soglia. | Age alarm scatta e notifica; al ripristino il messaggio viene elaborato. |
| OPS-RET-10 | P1 | Messaggio tecnicamente fallito in DLQ; causa rimossa; procedura redrive approvata. | Redrive verso source queue. | Una sola elaborazione finale riuscita; DLQ torna vuota; audit della manovra disponibile. |
| OPS-RET-11 | P1 | Messaggio sintatticamente invalido e uno funzionalmente non eleggibile. | Inviarli e osservare oltre il visibility timeout. | Entrambi ACKati una volta, non ritentati e non inviati in DLQ. |

## 13. Regressione US-1 sui flussi ordinari

Questi casi non devono usare la coda resume. Avviare il normale workflow che porta al metodo PREPARE o SEND indicato. Per ogni caso il `PAYMENT` e gia presente: e proprio la condizione che non deve piu bloccare.

### 13.1 Pagata ma non visualizzata e non cancellata

| ID | Pri | Precondizioni | Azione ordinaria | Risultato atteso |
| --- | --- | --- | --- | --- |
| REG-US1-01 | P0 | Notifica analogica al primo tentativo; pagata; non vista; non cancellata; pronta per PREPARE. | Far scattare PREPARE tentativo 0. | PREPARE e chiamata Paper eseguiti: il pagamento non blocca. |
| REG-US1-02 | P0 | Notifica analogica pronta al secondo tentativo; pagata; non vista; non cancellata. | Far scattare PREPARE tentativo 1. | PREPARE eseguito. |
| REG-US1-03 | P0 | Raccomandata semplice pronta per PREPARE; pagata; non vista; non cancellata. | Far scattare PREPARE semplice. | PREPARE eseguito. |
| REG-US1-04 | P0 | Analogica con PREPARE completato e risposta pronta per SEND; nel frattempo pagata; non vista; non cancellata. | Far scattare SEND analogico. | SEND a Paper eseguito. |
| REG-US1-05 | P0 | Semplice con PREPARE completato e pronta per SEND; pagata; non vista; non cancellata. | Far scattare SEND semplice. | SEND a Paper eseguito. |

### 13.2 Visualizzazione e cancellazione continuano a bloccare

| ID | Pri | Precondizioni | Azione ordinaria | Risultato atteso |
| --- | --- | --- | --- | --- |
| REG-US1-06 | P0 | Variante di ciascun PREPARE `REG-US1-01/02/03`, ma destinatario visto. | Tentare il PREPARE. | Nessuna chiamata Paper per tutti e tre i percorsi. |
| REG-US1-07 | P0 | Variante di ciascun SEND `REG-US1-04/05`, ma destinatario visto prima del SEND. | Tentare il SEND. | Nessun SEND Paper per entrambi i percorsi. |
| REG-US1-08 | P0 | Variante di ciascun PREPARE, ma cancellazione richiesta. | Tentare il PREPARE. | Nessuna chiamata Paper per tutti i percorsi. |
| REG-US1-09 | P0 | Variante di ciascun SEND, ma cancellazione richiesta prima del SEND. | Tentare il SEND. | Nessun SEND Paper per entrambi i percorsi. |
| REG-US1-10 | P1 | Pagata e vista contemporaneamente. | Tentare PREPARE e SEND nei percorsi applicabili. | La visualizzazione blocca; il pagamento non annulla il blocco. |

Il gate GATE-02 e superato solo se i cinque casi pagati/non visti procedono e tutti i controlli visto/cancellato bloccano.

## 14. Test end-to-end

Gli E2E partono dal CSV dello script e terminano con evidenza nei sistemi downstream. Usare un CSV dedicato per scenario e archiviare tutti i `MessageId`.

| ID | Pri | Precondizioni | Azione | Risultato atteso end-to-end |
| --- | --- | --- | --- | --- |
| E2E-FST-01 | P0 | `BASE-FIRST`; contratto env-only valido; servizi sani. | Inserire la coppia nel CSV FIRST ed eseguire lo script. | Script exit 0; messaggio consumato; success FIRST; PREPARE tentativo 0 e richiesta Paper presenti; coda vuota. |
| E2E-SND-01 | P0 | `BASE-SECOND-KO`; servizi sani. | Eseguire CSV SECOND. | Success SECOND; PREPARE tentativo 1 e Paper presenti; nessun duplicato. |
| E2E-SND-02 | P0 | `BASE-SECOND-TIMEOUT`; servizi sani. | Eseguire CSV SECOND. | Stesso esito positivo tramite trigger timeout. |
| E2E-SRL-01 | P0 | `BASE-SIMPLE`; caso non gia perfezionato secondo classificazione business. | Eseguire CSV SIMPLE. | Success SIMPLE; PREPARE semplice e Paper presenti. |
| E2E-SRL-02 | P0 | Candidato semplice gia perfezionato, identificato dalla query/dal criterio business; decisione GATE-03 formalizzata. | Eseguire CSV SIMPLE. | Esito economico e operativo conforme alla decisione formalizzata; nessuna doppia contabilizzazione. |
| E2E-SRL-03 | P1 | Due candidati semplici equivalenti salvo stato perfezionato/non perfezionato. | Elaborarli e confrontare le evidenze. | Differenze limitate a quelle approvate dal business; entrambe tracciabili. |
| E2E-MIX-01 | P0 | CSV con una eleggibile, una non eleggibile, una malformed e una duplicata; servizi sani. | Eseguire lo script. | Script pubblica le uniche valide; malformed/duplicate nei contatori; consumer completa la sana e ACKa lo scarto funzionale; nessun retry inatteso. |
| E2E-IDM-01 | P0 | Un E2E positivo gia concluso. | Rieseguire lo stesso CSV. | Script ripubblica una volta nella nuova run; applicazione scarta `PREPARE_ALREADY_PRESENT`; nessuna seconda Paper. |
| E2E-TECH-01 | P0 | Baseline positiva; un downstream indisponibile per un tentativo; finestra Operations. | Eseguire script, poi ripristinare il downstream. | Script conferma solo la pubblicazione; applicazione ritenta e conclude dopo il ripristino; questa distinzione e visibile nelle evidenze. |
| E2E-EXC-01 | P0 | Caso con PREPARE gia presente e SEND mancante. | Eseguire il CSV corrispondente. | Scarto idempotente; nessun PREPARE duplicato; caso consegnato alla procedura operativa separata. |

## 15. Verifica dei log e della privacy

| ID | Pri | Precondizioni | Azione | Risultato atteso |
| --- | --- | --- | --- | --- |
| OBS-LOG-01 | P1 | Un successo per ogni tipo. | Cercare i log per IUN. | Un `RESUME_POST_PAYMENT_SUCCESS` con IUN, recIndex, tipo, outcome e operation corretta. |
| OBS-LOG-02 | P1 | Uno scarto per ogni reason code. | Cercare i log per IUN. | `RESUME_POST_PAYMENT_ERROR` contiene reason code, descrizione e categorie Timeline ordinate; nessun dato personale non necessario. |
| OBS-LOG-03 | P1 | Payload sintatticamente invalido. | Cercare per `MessageId`/finestra temporale. | Errore diagnosticabile senza serializzare credenziali o intero contenuto sensibile. |
| OBS-LOG-04 | P1 | Errore tecnico e retry. | Correlare tentativi. | Processo marcato fallito a ogni errore; tentativi distinguibili; unico successo finale. |
| OBS-LOG-05 | P1 | Run script con successo parziale. | Analizzare summary. | Contatori invarianti rispettati; errori per riga/record correlabili; profilo, token e credenziali non esposti. |

## 16. Selezione dati tramite query operative

Le query previste da US-4 devono essere validate separatamente prima di usare i risultati come input dello script.

| ID | Pri | Precondizioni | Azione | Risultato atteso |
| --- | --- | --- | --- | --- |
| DATA-QRY-01 | P0 | Dataset campione FIRST con positivi e negativi noti. | Eseguire la query FIRST e verificare manualmente un campione Timeline. | Inclusi solo candidati con pagamento, schedule sufficientemente vecchio e senza PREPARE tentativo 0; nessun falso positivo nel campione. |
| DATA-QRY-02 | P0 | Dataset campione SECOND con KO, timeout, feedback positivo e SEND assente. | Eseguire la query SECOND. | Inclusi KO/timeout con SEND tentativo 0 e senza PREPARE tentativo 1; esclusi gli altri. |
| DATA-QRY-03 | P0 | Dataset SIMPLE con indirizzo presente/assente, refined/non-refined e PREPARE presente/assente. | Eseguire la query SIMPLE. | Classificazione corretta e separazione refined/non-refined disponibile per GATE-03. |
| DATA-QRY-04 | P1 | IUN con piu destinatari in stati diversi. | Eseguire tutte le query applicabili. | Output a livello `(iun, recIndex)`; nessuna contaminazione tra destinatari. |
| DATA-QRY-05 | P1 | Output delle query disponibile. | Validare header, tipi, duplicati e cardinalita prima dello script. | CSV esatto `iun,recIndex`, `recIndex` intero non negativo, coppie deduplicate e conteggi riconciliati. |

## 17. Matrice reason code

Ogni reason code deve essere osservato almeno una volta in test automatico o in DEV/QA. `VALID` e provato dai casi sani.

| Reason code | Test principale |
| --- | --- |
| `VALID` | `APP-FST-01`, `APP-SND-01/02`, `APP-SRL-01` |
| `RECIPIENT_NOT_FOUND` | `APP-COM-04` |
| `NOTIFICATION_IUN_MISMATCH` | `APP-COM-03` |
| `PAYMENT_NOT_FOUND` | `APP-COM-05/06` |
| `NOTIFICATION_CANCELLED` | `APP-COM-07` |
| `NOTIFICATION_VIEWED` | `APP-COM-08` |
| `NOTIFICATION_REWORKED` | `APP-COM-09` |
| `PREPARE_ALREADY_PRESENT` | `APP-FST-06`, `APP-SND-07`, `APP-SRL-05` |
| `SCHEDULE_ANALOG_WORKFLOW_NOT_FOUND` | `APP-FST-02` |
| `SCHEDULE_ANALOG_WORKFLOW_TOO_RECENT` | `APP-FST-03/05` |
| `FIRST_ANALOG_SEND_NOT_FOUND` | `APP-SND-03` |
| `SECOND_ATTEMPT_TRIGGER_NOT_FOUND` | `APP-SND-04/05` |
| `DIGITAL_FAILURE_WORKFLOW_NOT_FOUND` | `APP-SRL-02` |
| `SCHEDULE_REFINEMENT_NOT_FOUND` | `APP-SRL-03` |
| `PHYSICAL_ADDRESS_NOT_FOUND` | `APP-SRL-04` |

## 18. Tracciabilita requisiti

| Requisito | Copertura |
| --- | --- |
| US-1: pagamento non bloccante, visto/cancellato bloccanti | `REG-US1-01` - `REG-US1-10`, GATE-02 |
| US-2: consumer dedicato e contratto messaggio | `CON-SYN-01` - `CON-SYN-10` |
| US-2: regole comuni | `APP-COM-01` - `APP-COM-11` |
| US-2: ripresa primo tentativo | `APP-FST-01` - `APP-FST-07`, `E2E-FST-01` |
| US-2: ripresa secondo tentativo | `APP-SND-01` - `APP-SND-08`, `E2E-SND-01/02` |
| US-2: ripresa raccomandata semplice | `APP-SRL-01` - `APP-SRL-06`, `E2E-SRL-01/02/03` |
| US-2: idempotenza | `APP-IDM-01` - `APP-IDM-07`, `E2E-IDM-01` |
| US-2: ACK funzionale e retry tecnico | `CON-SYN-*`, `APP-COM-*`, `OPS-RET-01` - `OPS-RET-11` |
| US-2: log strutturati | `OBS-LOG-01` - `OBS-LOG-05` |
| US-3: invocazione e mapping CSV | `SCR-ARG-*`, `SCR-MAP-01` |
| US-3: configurazione AWS env-only | `SCR-CFG-01` - `SCR-CFG-11`, GATE-01 |
| US-3: validazione e deduplica CSV | `SCR-CSV-*`, `SCR-ROW-*` |
| US-3: invio sequenziale, errori e summary | `SCR-PUB-01` - `SCR-PUB-07` |
| US-4: estrazione candidati | `DATA-QRY-01` - `DATA-QRY-05` |
| Infrastruttura: coda, DLQ e allarmi | `OPS-RET-07` - `OPS-RET-10` |
| Esclusione casi tra PREPARE e SEND | `APP-IDM-07`, `E2E-EXC-01` |

## 19. Ordine di esecuzione consigliato

1. Risolvere o accettare formalmente i gate `GATE-01`, `GATE-02` e `GATE-03`.
2. Validare le query e preparare i baseline senza pubblicare messaggi.
3. Eseguire i test script che non pubblicano, poi quelli con pubblicazione controllata.
4. Eseguire contratto consumer e scarti sintattici.
5. Eseguire regole comuni e specifiche, partendo dai casi negativi e terminando con i positivi.
6. Eseguire idempotenza e concorrenza.
7. Eseguire la regressione US-1 sui flussi ordinari.
8. Eseguire gli E2E positivi e misti.
9. Nella finestra Operations, eseguire retry, DLQ, redrive e allarmi.
10. Riconciliare Timeline, richieste Paper, code e log; allegare le evidenze al test report.

## 20. Template di verbale del singolo test

```text
Test ID:
Titolo:
Ambiente / build:
Data e operatore:
Precondizioni verificate:
IUN / recIndex / resumeType:
Input o CSV:
Passi eseguiti:
Risultato atteso:
Risultato osservato:
MessageId / correlazioni:
Evidenze Timeline:
Evidenze Paper Channel:
Evidenze log e metriche:
Pulizia eseguita:
Esito: PASS | FAIL | BLOCKED
Difetto / note:
```
