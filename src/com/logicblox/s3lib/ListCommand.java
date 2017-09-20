package com.logicblox.s3lib;

import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class ListCommand extends Command {

  private ListOptions _options;

  public ListCommand(ListOptions options) {
    super(options);
    _options = options;
  }

  public ListenableFuture<List<StoreFile>> run() {
    ListenableFuture<List<StoreFile>> future =
        executeWithRetry(_client.getInternalExecutor(), new Callable<ListenableFuture<List<StoreFile>>>() {
          public ListenableFuture<List<StoreFile>> call() {
            return runActual();
          }
          
          public String toString() {
            return "listing objects and directories for "
                + getUri(_options.getBucketName(), _options.getObjectKey().orElse(""));
          }
        });
    
    return future;
  }
  
  private ListenableFuture<List<StoreFile>> runActual() {
    return _client.getApiExecutor().submit(new Callable<List<StoreFile>>() {

      public List<StoreFile> call() {
        ListObjectsRequest req = new ListObjectsRequest()
            .withBucketName(_options.getBucketName())
            .withPrefix(_options.getObjectKey().orElse(null));
        if (! _options.isRecursive()) {
          req.setDelimiter("/");
        }

        List<StoreFile> all = new ArrayList<StoreFile>();
        ObjectListing current = getAmazonS3Client().listObjects(req);
        appendS3ObjectSummaryList(all, current.getObjectSummaries());
        if (! _options.dirsExcluded()) {
          appendS3DirStringList(all, current.getCommonPrefixes(), _options.getBucketName());
        }
        current = getAmazonS3Client().listNextBatchOfObjects(current);
        
        while (current.isTruncated()) {
          appendS3ObjectSummaryList(all, current.getObjectSummaries());
          if (! _options.dirsExcluded()) {
            appendS3DirStringList(all, current.getCommonPrefixes(), _options.getBucketName());
          }
          current = getAmazonS3Client().listNextBatchOfObjects(current);
        }
        appendS3ObjectSummaryList(all, current.getObjectSummaries());
        if (! _options.dirsExcluded()) {
          appendS3DirStringList(all, current.getCommonPrefixes(), _options.getBucketName());
        }
        
        return all;
      }
    });
  }
  
  private List<StoreFile> appendS3ObjectSummaryList(
      List<StoreFile> all,
      List<S3ObjectSummary> appendList) {
    for (S3ObjectSummary o : appendList) {
      all.add(S3ObjectSummaryToStoreFile(o));
    }
    
    return all;
  }
  
  private List<StoreFile> appendS3DirStringList(
      List<StoreFile> all,
      List<String> appendList,
      String bucket) {
    for (String o : appendList) {
      all.add(S3DirStringToStoreFile(o, bucket));
    }
    
    return all;
  }
  
  private StoreFile S3ObjectSummaryToStoreFile(S3ObjectSummary o) {
    StoreFile of = new StoreFile();
    of.setKey(o.getKey());
    of.setETag(o.getETag());
    of.setBucketName(o.getBucketName());
    of.setSize(o.getSize());
    return of;
  }
  
  private StoreFile S3DirStringToStoreFile(String dir, String bucket) {
    StoreFile df = new StoreFile();
    df.setKey(dir);
    df.setBucketName(bucket);
    df.setSize(new Long(0));
    
    return df;
  }
}
