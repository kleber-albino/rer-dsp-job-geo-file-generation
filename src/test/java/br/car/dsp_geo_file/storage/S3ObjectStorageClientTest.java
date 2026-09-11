package br.car.dsp_geo_file.storage;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class S3ObjectStorageClientTest {

    private static final String BUCKET = "dsp-geo-files";

    private final S3Client s3Client = mock(S3Client.class);
    private final S3ObjectStorageClient client = new S3ObjectStorageClient(s3Client, BUCKET);

    @Test
    void putFile_SendsStagingFileToBucket(@org.junit.jupiter.api.io.TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("sao-paulo_area_of_interest.csv");
        Files.write(file, new byte[]{4, 5, 6});

        client.putFile("csv/level-2/sao-paulo_area_of_interest.csv",
                file,
                "text/csv;charset=UTF-8",
                Map.of("last-update", "2026-03-04T10:00:00Z"));

        ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(request.capture(), any(RequestBody.class));
        assertEquals(BUCKET, request.getValue().bucket());
        assertEquals("csv/level-2/sao-paulo_area_of_interest.csv", request.getValue().key());
        assertEquals("text/csv;charset=UTF-8", request.getValue().contentType());
        assertEquals(Map.of("last-update", "2026-03-04T10:00:00Z"), request.getValue().metadata());
    }

    @Test
    void head_ReadsSizeLastModifiedAndUserMetadata() {
        Instant lastModified = Instant.parse("2026-03-04T10:00:00Z");
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(
                HeadObjectResponse.builder()
                        .contentLength(42L)
                        .lastModified(lastModified)
                        .metadata(Map.of("last-update", "2026-03-01T00:00:00Z"))
                        .build());

        Optional<StoredObject> object = client.head("csv/level-2/sao-paulo_area_of_interest.csv");

        assertTrue(object.isPresent());
        assertEquals(42L, object.get().size());
        assertEquals(lastModified, object.get().lastModified());
        assertEquals("2026-03-01T00:00:00Z", object.get().userMetadata().get("last-update"));
    }

    @Test
    void head_ReturnsEmptyForAMissingKey() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().build());

        assertTrue(client.head("csv/level-2/missing.csv").isEmpty());
    }

    @Test
    void head_TreatsA404AsMissingBecauseSomeEndpointsDoNotTypeIt() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow((S3Exception) S3Exception.builder().statusCode(404).build());

        assertTrue(client.head("csv/level-2/missing.csv").isEmpty());
    }

    @Test
    void head_WrapsAnyOtherFailure() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow((S3Exception) S3Exception.builder().statusCode(500).build());

        assertThrows(ObjectStorageException.class, () -> client.head("csv/level-2/x.csv"));
    }

    @Test
    void bucketExists_FalseWhenTheBucketIsNotThere() {
        when(s3Client.headBucket(any(software.amazon.awssdk.services.s3.model.HeadBucketRequest.class)))
                .thenThrow(NoSuchBucketException.builder().build());

        assertFalse(client.bucketExists());
    }

    @Test
    void get_ReturnsEmptyForAMissingKey() {
        when(s3Client.getObjectAsBytes(
                any(software.amazon.awssdk.services.s3.model.GetObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().build());

        assertTrue(client.get("csv/level-2/missing.csv").isEmpty());
    }
}
