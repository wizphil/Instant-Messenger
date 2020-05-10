FROM rabbitmq:3
RUN rabbitmq-plugins enable --offline rabbitmq_management rabbitmq_event_exchange