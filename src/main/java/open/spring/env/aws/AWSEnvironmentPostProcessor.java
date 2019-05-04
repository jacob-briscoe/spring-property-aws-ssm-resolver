package open.spring.env.aws;

import java.util.Arrays;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import static java.util.function.Function.identity;
import java.util.function.Predicate;
import static java.util.stream.Collectors.*;
import open.spring.aws.AWSPropertySource;
import open.spring.aws.SSM;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.stereotype.Component;
import static org.springframework.util.StringUtils.startsWithIgnoreCase;

@Component
public class AWSEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String OVERRIDE_PROPERTYSOURCE_NAME = "override-%s";
    private static AWSPropertySource awsPropertySource;
    private static final Predicate<PropertySource> IS_ENUMERABLE = (propertySource) -> propertySource instanceof EnumerablePropertySource;
    private static final BiPredicate<String, EnumerablePropertySource> IS_PROPERTY_VALUE_STRING = (propertyName, enumerablePropertySource) -> enumerablePropertySource.getProperty(propertyName) instanceof String;
    private static final BiPredicate<String, EnumerablePropertySource> IS_PROPERTY_ELIGIBLE_FOR_OVERRIDE = determinOverrideEligibility();
    private static final Function<PropertySource, EnumerablePropertySource> CONVERT_TO_ENUMERABLE = (propertySource) -> (EnumerablePropertySource) propertySource;

    public AWSEnvironmentPostProcessor() {
        this(SSM.store());
    }

    @Override
    public void postProcessEnvironment(final ConfigurableEnvironment environment, final SpringApplication application) {
        environment.getPropertySources().stream()
                .filter(IS_ENUMERABLE)
                .map(CONVERT_TO_ENUMERABLE)
                .forEach(resolveAllPropertySources(environment));
    }

    private static Consumer<EnumerablePropertySource> resolveAllPropertySources(final ConfigurableEnvironment environment) {
        return (enumerablePropertySource) -> {
            final Map<String, Object> propertyOverrides = Arrays.stream(enumerablePropertySource.getPropertyNames())
                    .filter(propertyName -> IS_PROPERTY_VALUE_STRING.test(propertyName, enumerablePropertySource))
                    .filter(propertyName -> IS_PROPERTY_ELIGIBLE_FOR_OVERRIDE.test(propertyName, enumerablePropertySource))
                    .collect(toMap(identity(), propertyName -> resolveProperty(environment, enumerablePropertySource, propertyName)));
            
            maybeOverrideProperties(propertyOverrides, environment, enumerablePropertySource);
        };
    }

    private static void maybeOverrideProperties(final Map<String, Object> propertyOverrides, 
            final ConfigurableEnvironment environment, EnumerablePropertySource enumerablePropertySource) {
        if (propertyOverrides.isEmpty())
            return;

        environment.getPropertySources().addBefore(enumerablePropertySource.getName(),
                new MapPropertySource(nameForMapPropertySource(enumerablePropertySource), propertyOverrides));
    }

    private static String nameForMapPropertySource(EnumerablePropertySource enumerablePropertySource) {
        return String.format(OVERRIDE_PROPERTYSOURCE_NAME, enumerablePropertySource.getName());
    }

    private static String resolveProperty(final ConfigurableEnvironment environment, 
            final EnumerablePropertySource enumerablePropertySource, final String propertyName) {
        return awsPropertySource.getProperty(environment.resolvePlaceholders((String) enumerablePropertySource.getProperty(propertyName)));
    }

    private static BiPredicate<String, EnumerablePropertySource> determinOverrideEligibility() {
        return (propertyName, enumerablePropertySource) -> startsWithIgnoreCase((String) enumerablePropertySource.getProperty(propertyName), awsPropertySource.getPropertyPrefix());
    }

    private AWSEnvironmentPostProcessor(final AWSPropertySource providedAWSPropertySource) {
        if (awsPropertySource == null)
            applyAwsPropertySource(providedAWSPropertySource);
    }

    static void applyAwsPropertySource(final AWSPropertySource overrideAWSPropertySource) {
        awsPropertySource = overrideAWSPropertySource;
    }

}
