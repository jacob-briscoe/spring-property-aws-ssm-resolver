package io.github.jb.aws;

public interface AWSPropertySource {

    String getProperty(String name);

    String getPropertyPrefix();

}
