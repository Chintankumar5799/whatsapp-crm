// package com.appointment.notification.config;

// import com.amazonaws.services.sqs.AmazonSQSAsync;
// import com.fasterxml.jackson.databind.ObjectMapper;
// import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
// import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.messaging.converter.MappingJackson2MessageConverter;
// import org.springframework.messaging.converter.MessageConverter;

// @Configuration
// public class AwsSqsConfig {

//     @Bean
//     public QueueMessagingTemplate queueMessagingTemplate(AmazonSQSAsync amazonSQSAsync) {
//         QueueMessagingTemplate template = new QueueMessagingTemplate(amazonSQSAsync);
//         template.setMessageConverter(messageConverter());
//         return template;
//     }

//     @Bean
//     public MessageConverter messageConverter() {
//         MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
//         converter.setSerializedPayloadClass(String.class);
//         converter.setObjectMapper(objectMapper());
//         return converter;
//     }

//     @Bean
//     public ObjectMapper objectMapper() {
//         ObjectMapper mapper = new ObjectMapper();
//         mapper.registerModule(new JavaTimeModule());
//         return mapper;
//     }
// }
