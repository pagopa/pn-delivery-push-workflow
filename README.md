# pn-delivery-push-workflow-service

## Indice
- [Descrizione](#descrizione)
- [Tecnologie Utilizzate](#tecnologie-utilizzate)
- [Architettura](#architettura)
- [Interfacce del Servizio](#interfacce-del-servizio)
- [Configurazioni](#configurazioni)
- [Allarmi e Monitoraggio](#allarmi-e-monitoraggio)
- [Esecuzione](#esecuzione)

## Descrizione

**pn-delivery-push-workflow-service** è un microservizio che gestisce l’intero workflow della notifica successivo alla fase di validazione nell’ecosistema `SEND`. Il servizio si occupa di orchestrare le diverse fasi del processo notificativo, integrando componenti sia digitali che analogici.
Riceve notifiche già validate e coordina le seguenti attività:
- **Invio di messaggi di cortesia ai destinatari**
- **Scelta del canale di notificazione (digitale o analogico)**
- **Gestione della logica di business per il workflow digitale**
- **Gestione parziale del workflow analogico (con delega delle logiche principali al microservizio `pn-paper-channel`)**
- **Perfezionamento della notifica**
- **Visualizzazione delle notifiche**
- **Gestione della notificazione di pagamento (inserimento informativa in timeline)**
- **Gestione asincrona dell’annullamento della notifica (la parte sincrona è gestita da un altro microservizio)**

Il workflow viene avviato tramite un unico punto di ingresso: al termine della validazione, un messaggio di tipo `POST_ACCEPTED_PROCESSING_COMPLETED` viene inviato sulla coda SQS pn-delivery-push-action. 
Il completamento del processo varia in base al percorso notificativo specifico di ciascuna notifica.

## Tecnologie Utilizzate

### Stack Tecnologico
- Java / Spring Boot 3 (Spring Cloud AWS, Spring Cloud Stream)
- Node.js (lambda `pn-notificationCancellationActionInsert-workflow`, lambda `pn-paperEventsCostUpdate-workflow`)
- AWS SDK (DynamoDB Enhanced Client)
- OpenAPI Generator (client/model generati da specifica)

### Infrastruttura
- Amazon SQS (code di input/output del workflow)
- Amazon DynamoDB (`PaperNotificationFailedDynamoTable`, `DocumentCreationRequestTable`)
- Amazon Kinesis Data Streams (`CdcKinesisSourceStream`, sorgente eventi per le lambda)
- AWS Lambda (`pn-notificationCancellationActionInsert-workflow`, `pn-paperEventsCostUpdate-workflow`)
- Amazon CloudWatch (log, alarm, dashboard)

## Architettura

Si compone di:
- **Microservizio pn-delivery-push-workflow-service**: gestisce l’orchestrazione del workflow notificativo successivo alla validazione, coordinando le interazioni tra i diversi canali (digitali e analogici) e i servizi esterni. Legge sulle code SQS DeliveryPushInputsQueue, ExternalChannelsOutputsQueue, ScheduledActionsQueue, NationalRegistries2DeliveryPushQueue e legge/scrive sulle tabelle DynamoDB PaperNotificationFailedDynamoTable, DocumentCreationRequestTable.
- **Lambda pn-notificationCancellationActionInsert-workflow**: si occupa della gestione asincrona delle richieste di annullamento notifica. Elabora messaggi dallo stream Kinesis `CdcKinesisSourceStream`, filtrando gli eventi di timeline relativi a una richiesta di annullamento notifica (`NOTIFICATION_CANCELLATION`), e inserisce le action richiamando `pn-action-manager`, che le smisterà in modo asincrono sul ms `pn-delivery-push-workflow` per portare avanti il processo di annullamento. N.B. la lambda utilizzerà un meccanismo di feature flag temporizzato, basato sull'`eventTimestamp` di kinesis, con i parametri `NewWorkflowLambdasEnabledStart` e `NewWorkflowLambdasEnabledEnd`.
- **Lambda pn-paperEventsCostUpdate-workflow**: aggiorna i costi degli eventi relativi alle notifiche cartacee. Elabora messaggi dallo stream Kinesis `CdcKinesisSourceStream`, estraendo per ogni evento le informazioni necessarie al calcolo/aggiornamento dei costi, e invia le informazioni aggiornate sulla coda SQS `DeliveryPushToExternalRegistriesQueue`, letta dal ms `pn-external-registries`. N.B. la lambda utilizzerà un meccanismo di feature flag temporizzato, basato sull'`eventTimestamp` di kinesis, con i parametri `NewWorkflowLambdasEnabledStart` e `NewWorkflowLambdasEnabledEnd`.

![Architettura.png](Architettura.png)
https://excalidraw.com/#json=Hk7Qa4AjNhfcMAS_fbEjZ,7_8WMPSLNGf3nP-s7MzuWQ

## Interfacce del Servizio

| Tipo  | Dir | Risorsa                                | Protocollo | Metodo  | Route | Descrizione                                                                                                    |
|-------|-----|-----------------------------------------|------------|---------|-------|------------------------------------------------------------------------------------------------------------------|
| EVENT | IN  | DeliveryPushInputsQueue                 | SQS        | CONSUME | -     | Gestisce eventi relativi al processo di visualizzazione o pagamento di una notifica                              |
| EVENT | IN  | NationalRegistries2DeliveryPushQueue    | SQS        | CONSUME | -     | Gestisce le risposte api asincrone di pn-national-registries per il recupero degli indirizzi digitali o cartacei |
| EVENT | IN  | ScheduledActionsQueue                   | SQS        | CONSUME | -     | Gestisce le action legate ai flussi del dominio di workflow                                                      |
| EVENT | IN  | ExternalChannelsOutputsQueue            | SQS        | CONSUME | -     | Gestisce tutti gli eventi relativi ai processi di invio notifiche su canali digitali o analogici                 |
| EVENT | OUT | DeliveryPushToExternalRegistriesQueue   | SQS        | PRODUCE | -     | Invia verso `pn-external-registries` le informazioni aggiornate sui costi delle notifiche cartacee (lambda pn-paperEventsCostUpdate-workflow) |

## Configurazioni

| Nome                                                          | Sorgente | Valori                     | Descrizione                                                                 |
|-----------------------------------------------------------------|----------|----------------------------|------------------------------------------------------------------------------|
| AWS_REGIONCODE                                                   | ENV      | Codice regione AWS         | Regione AWS utilizzata dal servizio                                          |
| PN_DELIVERYPUSHWORKFLOW_TOPICS_NEWNOTIFICATIONS                  | ENV      | Nome coda SQS              | Coda da cui vengono letti gli eventi di input del workflow                   |
| PN_DELIVERYPUSHWORKFLOW_TOPICS_NATIONALREGISTRIESEVENTS          | ENV      | Nome coda SQS              | Coda National Registries verso delivery-push                                 |
| PN_DELIVERYPUSHWORKFLOW_TOPICS_FROMEXTERNALCHANNEL               | ENV      | Nome coda SQS              | Coda da cui vengono letti i messaggi di external-channel                     |
| PN_DELIVERYPUSHWORKFLOW_TOPICS_SCHEDULEDACTIONS                  | ENV      | Nome coda SQS              | Coda per l'invio e la lettura delle action pronte per l'esecuzione           |
| PN_DELIVERYPUSHWORKFLOW_EXTERNALCHANNELBASEURL                   | ENV      | URL servizio               | Base url del servizio external-channel                                       |
| PN_DELIVERYPUSHWORKFLOW_PAPERCHANNELBASEURL                      | ENV      | URL servizio               | Base url del servizio paper-channel                                          |
| PN_DELIVERYPUSHWORKFLOW_USERATTRIBUTESBASEURL                    | ENV      | URL servizio               | Base url del servizio user-attributes                                        |
| PN_DELIVERYPUSHWORKFLOW_SAFESTORAGEBASEURL                       | ENV      | URL servizio               | Base url del servizio safe-storage                                           |
| PN_DELIVERYPUSHWORKFLOW_DELIVERYBASEURL                          | ENV      | URL servizio               | Base url del servizio delivery                                               |
| PN_DELIVERYPUSHWORKFLOW_MANDATEBASEURL                           | ENV      | URL servizio               | Base url del servizio mandate                                                |
| PN_DELIVERYPUSHWORKFLOW_EXTERNALREGISTRYBASEURL                  | ENV      | URL servizio               | Base url del servizio external-registry                                     |
| PN_DELIVERYPUSHWORKFLOW_TEMPLATESENGINEBASEURL                   | ENV      | URL servizio               | Base url del servizio templates-engine                                       |
| PN_DELIVERYPUSHWORKFLOW_TIMELINECLIENTBASEURL                    | ENV      | URL servizio               | Base url del client timeline                                                 |
| PN_DELIVERYPUSHWORKFLOW_EMDINTEGRATIONBASEURL                    | ENV      | URL servizio               | Base url del servizio emd-integration                                        |
| PN_DELIVERYPUSHWORKFLOW_ACTIONMANAGERBASEURL                     | ENV      | URL servizio               | Base url del servizio action-manager                                         |
| PN_DELIVERYPUSHWORKFLOW_NATIONALREGISTRIESBASEURL                | ENV      | URL servizio               | Base url del servizio national-registries                                    |
| PN_DELIVERYPUSHWORKFLOW_DATAVAULTBASEURL                         | ENV      | URL servizio               | Base url del servizio data-vault                                             |
| PN_DELIVERYPUSHWORKFLOW_DELIVERYPUSHBASEURL                      | ENV      | URL servizio               | Base url del servizio delivery-push                                          |
| PN_DELIVERYPUSHWORKFLOW_PAPERTRACKERBASEURL                      | ENV      | URL servizio               | Base url del servizio paper-tracker                                          |
| PN_DELIVERYPUSHWORKFLOW_DOCUMENTCREATIONREQUESTDAO_TABLENAME     | ENV      | Nome tabella DynamoDB      | Tabella per persistere le richieste di creazione dei legalFacts (lookup lato responseHandler SafeStorage) |
| PN_DELIVERYPUSHWORKFLOW_FAILEDNOTIFICATIONDAO_TABLENAME          | ENV      | Nome tabella DynamoDB      | Tabella per persistere le notifiche cartacee non consegnate                  |
| PN_CRON_ANALYZER                                                 | ENV      | Espressione cron           | Pianifica l'invio della metrica verso CloudWatch                             |
| REGION (pn-notificationCancellationActionInsert-workflow)        | ENV      | Codice regione AWS         | Regione AWS utilizzata dalla lambda                                          |
| ACTION_MANAGER_BASE_URL (pn-notificationCancellationActionInsert-workflow) | ENV | URL servizio        | Base url del servizio action-manager usata dalla lambda                      |
| REGION (pn-paperEventsCostUpdate-workflow)                       | ENV      | Codice regione AWS         | Regione AWS utilizzata dalla lambda                                          |
| QUEUE_URL (pn-paperEventsCostUpdate-workflow)                    | ENV      | URL coda SQS               | Coda verso `pn-external-registries` su cui la lambda invia i costi aggiornati |

## Allarmi e Monitoraggio

| Tipo      | Nome                                                        | Descrizione                                                                                             |
|-----------|-------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------|
| ALARM     | DeliveryPushInputsQueueAlarmARN / DeliveryPushInputsQueueAgeAlarmARN | Segnalano messaggi in DLQ o età eccessiva dei messaggi sulla coda DeliveryPushInputsQueue                 |
| ALARM     | ScheduledActionsQueueAlarmARN / ScheduledActionsQueueAgeAlarmARN | Segnalano messaggi in DLQ o età eccessiva dei messaggi sulla coda ScheduledActionsQueue                   |
| ALARM     | NationalRegistries2DeliveryPushQueueAlarmARN / NationalRegistries2DeliveryPushQueueAgeAlarmARN | Segnalano messaggi in DLQ o età eccessiva dei messaggi sulla coda NationalRegistries2DeliveryPushQueue |
| ALARM     | AlarmCustomAutoscalingWorkflow                               | Alarm su metrica custom usato per attivare l'autoscaling del servizio                                     |
| ALARM     | NotificationCancellationActionInsertKinesisFailuresAlarm     | Segnala errori di lettura dallo stream Kinesis da parte della lambda pn-notificationCancellationActionInsert-workflow |
| ALARM     | NotificationCancellationActionInsertLambdaAlarms (LambdaInvocationErrorLogsMetricAlarm) | Segnala errori di invocazione della lambda pn-notificationCancellationActionInsert-workflow |
| ALARM     | PaperEventsCostUpdateKinesisFailuresAlarm                    | Segnala errori di lettura dallo stream Kinesis da parte della lambda pn-paperEventsCostUpdate-workflow     |
| ALARM     | PaperEventsCostUpdateLambdaAlarms (LambdaInvocationErrorLogsMetricAlarm) | Segnala errori di invocazione della lambda pn-paperEventsCostUpdate-workflow       |
| DASHBOARD | DeliveryPushWorkflowServiceMicroserviceCloudWatchDashboard   | Dashboard CloudWatch con code, tabelle DynamoDB, lambda e alarm del servizio                               |

## Esecuzione

Prerequisiti:
```bash
java -version   # JDK richiesto dal parent pn-parent
./mvnw -v
```

Build:
```bash
./mvnw clean install
```

Test:
```bash
./mvnw test
```

Avvio locale:
```bash
./mvnw spring-boot:run
```
