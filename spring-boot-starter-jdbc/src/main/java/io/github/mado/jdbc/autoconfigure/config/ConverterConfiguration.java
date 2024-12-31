package io.github.mado.jdbc.autoconfigure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mado.jdbc.converter.*;
import io.github.mado.jdbc.converter.extension.BigIntToIntegerConverter;
import io.github.mado.jdbc.converter.extension.BigIntToLongConverter;
import io.github.mado.jdbc.converter.extension.IntegerToBooleanConverter;
import io.github.mado.jdbc.converter.extension.JsonPropertyValueConverter;
import io.github.mado.jdbc.core.executor.PropertyAccessorCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.convert.PropertyValueConversionService;
import org.springframework.data.convert.PropertyValueConversions;
import org.springframework.data.convert.PropertyValueConverterFactory;
import org.springframework.data.jdbc.core.convert.*;
import org.springframework.data.jdbc.core.dialect.JdbcDialect;
import org.springframework.data.jdbc.core.mapping.JdbcMappingContext;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;
import org.springframework.data.relational.RelationalManagedTypes;
import org.springframework.data.relational.core.dialect.Dialect;
import org.springframework.data.relational.core.mapping.DefaultNamingStrategy;
import org.springframework.data.relational.core.mapping.NamingStrategy;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * @author heng.ma
 */
@ConditionalOnClass(JsonPropertyValueConverter.class)
@Import(ConverterConfiguration.SmartJdbcConfiguration.class)
@Configuration(proxyBeanMethods = false)
public class ConverterConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public JsonPropertyValueConverter jsonPropertyValueConverter (ObjectMapper objectMapper) {
        return new JsonPropertyValueConverter(objectMapper);
    }

    @Bean
    public Consumer<GenericConversionService> innerConverterConsumer () {
        return conversionService -> {
            conversionService.addConverter(new BigIntToIntegerConverter());
            conversionService.addConverter(new BigIntToLongConverter());
            conversionService.addConverter(new IntegerToBooleanConverter());
        };
    }


    @Bean
    @Order
    public PropertyAccessorCustomizer propertyAccessorCustomizer (ConversionService conversionService, ApplicationContext applicationContext) {
        return accessor -> new SpecifyConvertingPropertyAccessor<>(accessor, conversionService, applicationContext);
    }


    @Bean
    @ConditionalOnMissingBean
    public PropertyValueConverterFactory propertyValueConverterFactory (ApplicationContext applicationContext) {
        return new SpecifyPropertyConverterFactory(applicationContext);
    }


    public class SmartJdbcConfiguration extends AbstractJdbcConfiguration {

        private ApplicationContext applicationContext;

        private final PropertyValueConversions propertyValueConversions;
        private final PropertyAccessorCustomizer propertyAccessorCustomizer;

        public SmartJdbcConfiguration(PropertyValueConversions propertyValueConversions, ConversionService conversionService, PropertyAccessorCustomizer propertyAccessorCustomizer) {
            this.propertyValueConversions = propertyValueConversions;
            this.propertyAccessorCustomizer = propertyAccessorCustomizer;
        }

        @Override
        public void setApplicationContext(ApplicationContext applicationContext) {
            super.setApplicationContext(applicationContext);
            this.applicationContext = applicationContext;
        }

        @Bean
        @ConditionalOnMissingBean
        public InsertStrategyFactory insertStrategyFactory(NamedParameterJdbcOperations operations, BatchJdbcOperations batchJdbcOperations, Dialect dialect) {
            return new InsertStrategyFactory(operations, batchJdbcOperations, dialect);
        }


        @Bean
        @Override
        public JdbcCustomConversions jdbcCustomConversions() {
            return new InternalConversions(applicationContext.getBean(Dialect.class), userConverters(), propertyValueConversions);
        }

        @Bean
        @ConditionalOnMissingBean
        public PropertyValueConversionService propertyValueConversionService(JdbcCustomConversions conversions) {
            return new PropertyValueConversionService(conversions);
        }

        @Bean
        @Override
        public JdbcMappingContext jdbcMappingContext(Optional<NamingStrategy> namingStrategy, JdbcCustomConversions customConversions, RelationalManagedTypes jdbcManagedTypes) {
            InternalMappingContext mappingContext = new InternalMappingContext(namingStrategy.orElse(DefaultNamingStrategy.INSTANCE));
            mappingContext.setSimpleTypeHolder(customConversions.getSimpleTypeHolder());
            mappingContext.setManagedTypes(jdbcManagedTypes);

            return mappingContext;
        }

        @Override
        @Bean
        public JdbcConverter jdbcConverter(JdbcMappingContext mappingContext,
                                           NamedParameterJdbcOperations operations,
                                           @Lazy RelationResolver relationResolver,
                                           JdbcCustomConversions conversions, Dialect dialect) {
            JdbcArrayColumns arrayColumns = dialect instanceof JdbcDialect ? ((JdbcDialect) dialect).getArraySupport()
                    : JdbcArrayColumns.DefaultSupport.INSTANCE;
            DefaultJdbcTypeFactory jdbcTypeFactory = new DefaultJdbcTypeFactory(operations.getJdbcOperations(), arrayColumns);
            return new InternalJdbcConverter(mappingContext, relationResolver, conversions, jdbcTypeFactory, dialect.getIdentifierProcessing(), propertyAccessorCustomizer);
        }
    }
}