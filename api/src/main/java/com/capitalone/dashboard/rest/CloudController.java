package com.capitalone.dashboard.rest;

import com.capitalone.dashboard.model.*;
import com.capitalone.dashboard.request.CloudRequest;
import com.capitalone.dashboard.request.CollectorItemRequest;
import com.capitalone.dashboard.service.CloudService;
import com.capitalone.dashboard.service.CollectorService;
import com.capitalone.dashboard.service.EncryptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

import static org.springframework.web.bind.annotation.RequestMethod.POST;

/**
 * REST service managing all requests to the feature repository.
 */
@RestController
public class CloudController {
    private final CloudService cloudService;
    private final EncryptionService encryptionService;
    private final CollectorService collectorService;
    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    @Autowired
    public CloudController(EncryptionService encryptionService,
                           CloudService cloudService, CollectorService collectorService) {
        this.cloudService = cloudService;
        this.encryptionService = encryptionService;
        this.collectorService = collectorService;
    }

    @RequestMapping(value = "/cloud/aggregate", method = POST, consumes = JSON, produces = JSON)
    public DataResponse<CloudComputeData> getAggregatedData(
            @Valid @RequestBody CloudRequest request) {
        return cloudService.getAggregatedData(request.getId());
    }

    @RequestMapping(value = "/cloud/details", method = POST, consumes = JSON, produces = JSON)
    public DataResponse<List<CloudComputeInstanceData>> getInstanceDetails(
            @Valid @RequestBody CloudRequest request) {
        return cloudService.getInstanceDetails(request.getId());
    }

    @RequestMapping(value = "/cloud/config", method = POST, consumes = JSON, produces = JSON)
    public ResponseEntity<CollectorItem> createCloudConfigCollectorItem(
            @Valid @RequestBody CollectorItemRequest request) {

        final String ACCESS_KEY = "accessKey";
        final String SECRET_KEY = "secretKey";
        final String PROVIDER = "cloudProvider";

        CollectorItem item = null;

        List<CollectorItem> items = collectorService.collectorItemsByType(CollectorType.Cloud);
        for (CollectorItem i : items) {
            if (i.getCollectorId().equals(request.getCollectorId()) && (request.getOptions().equals(i.getOptions()))) {
                item = i;
                break;
            }
        }

        if (item != null) {
            return ResponseEntity.status(HttpStatus.CREATED).body(item);
        } else {
            String encAccessKey = encryptionService.encrypt((String) request
                    .getOptions().get(ACCESS_KEY));
            String encSecretKey = encryptionService.encrypt((String) request
                    .getOptions().get(SECRET_KEY));
            if (!"ERROR".equalsIgnoreCase(encAccessKey)
                    && !"ERROR".equalsIgnoreCase(encSecretKey)) {
                request.getOptions().put(ACCESS_KEY, encAccessKey);
                request.getOptions().put(SECRET_KEY, encSecretKey);

                item = collectorService.createCollectorItem(request
                        .toCollectorItem());
                return ResponseEntity.status(HttpStatus.CREATED).body(item);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(request.toCollectorItem());
            }
        }
    }

}