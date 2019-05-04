package open.spring.aws;

public interface AWSPropertySource {

    String getProperty(String name);

    String getPropertyPrefix();

}
