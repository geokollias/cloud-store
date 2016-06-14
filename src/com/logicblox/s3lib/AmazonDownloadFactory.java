package com.logicblox.s3lib;

import java.util.concurrent.Callable;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.services.s3.model.GetObjectMetadataRequest;

class AmazonDownloadFactory
{
  private ListeningExecutorService executor;
  private AmazonS3 client;

  public AmazonDownloadFactory(AmazonS3 client, ListeningExecutorService
      executor)
  {
    this.client = client;
    this.executor = executor;
  }

  public ListenableFuture<AmazonDownload> startDownload(String bucketName, String key, String version)
  {
    return executor.submit(new StartCallable(bucketName, key, version));
  }

  private class StartCallable implements Callable<AmazonDownload>
  {
    private String bucketName;
    private String key;
    private String version;

    public StartCallable(String bucketName, String key, String version)
    {
      this.bucketName = bucketName;
      this.key = key;
      this.version = version;
    }

    public AmazonDownload call()
    {
      GetObjectMetadataRequest metareq = new GetObjectMetadataRequest(bucketName, key, version);
      ObjectMetadata data = client.getObjectMetadata(metareq);
      return new AmazonDownload(client, key, bucketName,version, data, executor);
    }
  }
}
