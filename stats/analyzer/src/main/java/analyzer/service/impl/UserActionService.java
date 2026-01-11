package analyzer.service.impl;

import analyzer.config.WeightProperties;
import analyzer.mapper.UserActionMapper;
import analyzer.model.ActionType;
import analyzer.model.UserAction;
import analyzer.repository.UserActionRepository;
import jakarta.transaction.Transactional;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserActionService implements analyzer.service.UserActionService {
    WeightProperties weightProperties;
    UserActionRepository userActionRepository;
    UserActionMapper userActionMapper;

    @Transactional
    @Override
    public void handleUserAction(UserActionAvro avro) {
        log.info("Сохраняем действие: {} пользователя: {} для события {}", avro, avro.getUserId(), avro.getEventId());
        Optional<UserAction> userActionOpt = userActionRepository.findByUserIdAndEventId(avro.getUserId(), avro.getEventId());
        ActionType newType = avroTypeToEntity(avro.getActionType());
        double newWeight = getWeightForAction(newType);

        if (userActionOpt.isPresent()) {
            UserAction userAction = userActionOpt.get();

            if (Double.compare(newWeight, userAction.getActionWeight()) > 0) {
                userAction.setActionType(newType);
                userAction.setTimestamp(avro.getTimestamp());
                userAction.setActionWeight(newWeight);
                userActionRepository.save(userAction);
            }
            return;
        }
        UserAction userAction = userActionMapper.AvroToEntity(avro, newWeight);
        userAction.setActionType(newType);
        userActionRepository.save(userAction);
    }

    private ActionType avroTypeToEntity(ActionTypeAvro avroType) {
        return switch (avroType) {
            case VIEW   -> ActionType.VIEW;
            case REGISTER -> ActionType.REGISTER;
            case LIKE   -> ActionType.LIKE;
        };
    }

    private double getWeightForAction(ActionType actionType) {
        return switch (actionType) {
            case VIEW -> weightProperties.getView();
            case REGISTER -> weightProperties.getRegister();
            case LIKE -> weightProperties.getLike();
        };
    }
}
