package com.iexec.commons.poco.itest;

import com.iexec.commons.poco.chain.AbstractAssetDeploymentService;
import com.iexec.commons.poco.chain.SignerService;

public class AssetDeploymentService extends AbstractAssetDeploymentService {
    protected AssetDeploymentService(SignerService signerService, String assetRegistrySelector) {
        super(signerService, assetRegistrySelector);
    }
}
