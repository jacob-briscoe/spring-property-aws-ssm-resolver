package open.spring.env.aws;

import open.spring.aws.AWSPropertySource;
import open.spring.aws.SSM;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import static org.mockito.Mockito.when;

@SpringBootTest
public class AWSEnvironmentPostProcessorTest {

    private static AWSPropertySource awsPropertySource;

    @Value("${someproperty}")
    private int someProperty;

    @Value("${anotherproperty}")
    private String anotherProperty;

    @Value("${thisComplexPath}")
    private String thisComplexPath;

    @Value("${simplePropFromAWSSSM}")
    private String simplePropFromAWSSSM;
    private static final String SIMPLE_PROP_FROM_AWSSSM_EXPECTED = "username";

    @Value("${springRefPropFromAWSSSM}")
    private String springRefPropFromAWSSSM;
    private static final String SPRING_REF_PROP_FROM_AWSSSM_EXPECTED = "local_user";

    @BeforeAll
    public static void setup() {
        awsPropertySource = mock(AWSPropertySource.class);
        AWSEnvironmentPostProcessor.applyAwsPropertySource(awsPropertySource);

        when(awsPropertySource.getPropertyPrefix()).thenReturn(SSM.PREFIX);
        when(awsPropertySource.getProperty(String.format("%s/db/user", SSM.PREFIX))).thenReturn(SIMPLE_PROP_FROM_AWSSSM_EXPECTED);
        when(awsPropertySource.getProperty(String.format("%s/db/${spring.profiles.active}/user", SSM.PREFIX))).thenReturn(SPRING_REF_PROP_FROM_AWSSSM_EXPECTED);
    }

    @Test
    public void simpleProperties() {
        assertEquals(123, someProperty);
        assertEquals("hello, world", anotherProperty);
        assertEquals(String.format("/%d/%s", someProperty, "4"), thisComplexPath);
    }

    @Test
    public void awsSSMProperties() {
        assertEquals(SIMPLE_PROP_FROM_AWSSSM_EXPECTED, simplePropFromAWSSSM);
        assertEquals(SPRING_REF_PROP_FROM_AWSSSM_EXPECTED, springRefPropFromAWSSSM);
    }

    @Configuration
    static class AWSEnvironmentPostProcessorTestConfiguration {

        @Bean
        PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
            return new PropertySourcesPlaceholderConfigurer();
        }

    }

}
