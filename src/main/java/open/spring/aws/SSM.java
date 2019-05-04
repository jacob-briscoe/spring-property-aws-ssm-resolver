package open.spring.aws;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest;

public class SSM implements AWSPropertySource {

    public static final String PREFIX = "{ssmParameter}";
    private AWSSimpleSystemsManagement ssm;

    public static SSM store() {
        return new SSM();
    }

    @Override
    public String getProperty(final String name) {
        return getAWSClient().getParameter(createPropertyRequest(name)).getParameter().getValue();
    }

    @Override
    public String getPropertyPrefix() {
        return PREFIX;
    }

    private static GetParameterRequest createPropertyRequest(final String name) {
        return new GetParameterRequest().withName(extractParameterName(name)).withWithDecryption(true);
    }

    private static String extractParameterName(final String name) {
        return name.substring(PREFIX.length());
    }

    /**
     * Lazily initiated AWS client to prevent errors when the configuration has no ssm configured parameters and no working AWS environment setup
     *
     * @return AWS SSM client
     */
    private AWSSimpleSystemsManagement getAWSClient() {
        if (ssm == null)
            ssm = AWSSimpleSystemsManagementClientBuilder.defaultClient();

        return ssm;
    }

    private SSM() {

    }

}
