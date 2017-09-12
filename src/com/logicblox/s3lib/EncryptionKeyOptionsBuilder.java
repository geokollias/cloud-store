package com.logicblox.s3lib;

public class EncryptionKeyOptionsBuilder
{
  private CloudStoreClient _cloudStoreClient;
  private String _bucket;
  private String _objectKey;
  private String _encryptionKey;


  public EncryptionKeyOptionsBuilder setCloudStoreClient(CloudStoreClient client)
  {
    _cloudStoreClient = client;
    return this;
  }

  public EncryptionKeyOptionsBuilder setBucket(String bucket)
  {
    _bucket = bucket;
    return this;
  }
  
  public EncryptionKeyOptionsBuilder setObjectKey(String objectKey)
  {
    _objectKey = objectKey;
    return this;
  }
  
  public EncryptionKeyOptionsBuilder setEncryptionKey(String encryptionKey)
  {
    _encryptionKey = encryptionKey;
    return this;
  }

  public EncryptionKeyOptions createEncryptionKeyOptions()
  {
    return new EncryptionKeyOptions(_cloudStoreClient, _bucket, _objectKey,
      _encryptionKey);
  }
}
