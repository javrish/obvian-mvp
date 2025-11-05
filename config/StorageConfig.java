package api.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configuration for S3 storage
 */
@Configuration
public class StorageConfig {
    
    @Value("${aws.s3.endpoint:http://localhost:9000}")
    private String s3Endpoint;
    
    @Value("${aws.s3.region:us-east-1}")
    private String s3Region;
    
    @Value("${aws.s3.access-key:minioadmin}")
    private String accessKey;
    
    @Value("${aws.s3.secret-key:minioadmin}")
    private String secretKey;
    
    /**
     * Development/test S3 client configuration
     * Uses MinIO or LocalStack in development
     */
    @Bean
    @Profile({"development", "test", "default"})
    public AmazonS3 amazonS3Client() {
        BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
        
        return AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(
                    new AwsClientBuilder.EndpointConfiguration(s3Endpoint, s3Region))
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withPathStyleAccessEnabled(true)
                .build();
    }
    
    /**
     * Production S3 client configuration
     * Uses real AWS S3 in production
     */
    @Bean
    @Profile("production")
    public AmazonS3 amazonS3ClientProduction() {
        BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
        
        return AmazonS3ClientBuilder.standard()
                .withRegion(s3Region)
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .build();
    }
}