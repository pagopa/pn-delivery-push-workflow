package it.pagopa.pn.deliverypushworkflow.service.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.ElementTimestampTimelineElementDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.stereotype.Component;

import java.io.IOException;


@Slf4j
@Component
@RequiredArgsConstructor
public class SmartMapper {
    private static ModelMapper modelMapper;
    private final ObjectMapper objectMapper;

    static Converter<TimelineElementInternal, TimelineElementInternal> timelineElementInternalTimestampConverter =
            ctx -> {
                // se il detail estende l'interfaccia e l'elementTimestamp non è nullo, lo sovrascrivo nel source originale
                if (ctx.getSource().getDetails() instanceof ElementTimestampTimelineElementDetails elementTimestampTimelineElementDetails
                    && elementTimestampTimelineElementDetails.getElementTimestamp() != null)
                {
                    return ctx.getSource().toBuilder()
                            .timestamp(elementTimestampTimelineElementDetails.getElementTimestamp())
                            .build();
                }

                return ctx.getSource();
            };

    static{
        modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        modelMapper.createTypeMap(TimelineElementInternal.class, TimelineElementInternal.class).setPostConverter(timelineElementInternalTimestampConverter);
    }

    /*
        Mapping effettuato per la modifica dei timestamp per gli
        elementi di timeline che implementano l'interfaccia ElementTimestampTimelineElementDetails
     */
    public static  <S,T> T mapToClass(S source, Class<T> destinationClass ){
        T result;
        if( source != null) {
            result = modelMapper.map(source, destinationClass );
        } else {
            result = null;
        }
        return result;
    }

    public <S,T> T mapToClassWithObjectMapper(S source, Class<T> destinationClass )  {
        T result;
        try {
            if( source != null) {
                result = objectMapper.readValue(objectMapper.writeValueAsBytes(source), destinationClass);
            } else {
                result = null;
            }
        } catch (IOException e) {
            throw new PnInternalException("Errore durante il mapping del dettaglio", "MAPPING_ERROR", e);
        }

        return result;
    }

}
