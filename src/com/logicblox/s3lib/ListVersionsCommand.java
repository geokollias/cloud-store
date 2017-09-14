package com.logicblox.s3lib;

import com.amazonaws.services.s3.model.VersionListing;

import com.amazonaws.services.s3.model.S3VersionSummary;
import com.amazonaws.services.s3.model.ListVersionsRequest;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.amazonaws.services.s3.internal.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class ListVersionsCommand extends Command {
  
  private ListOptions _options;
  private ListeningExecutorService _httpExecutor;
  private ListeningScheduledExecutorService _executor;

  public ListVersionsCommand(ListOptions options) {
    _options = options;
    _httpExecutor = _options.getCloudStoreClient().getApiExecutor();
    _executor = _options.getCloudStoreClient().getInternalExecutor();
  }
  
  public ListenableFuture<List<S3File>> run() {
    ListenableFuture<List<S3File>> future =
        executeWithRetry(_executor, new Callable<ListenableFuture<List<S3File>>>() {
          
          
          public ListenableFuture<List<S3File>> call() {
            return runActual();
          }
          
          public String toString() {
            return "listing objects and directories for "
                + getUri(_options.getBucketName(), _options.getObjectKey().orElse(""));
          }
        });
    
    return future;
  }
  
  private ListenableFuture<List<S3File>> runActual() {
    return _httpExecutor.submit(new Callable<List<S3File>>() {
      
      
      public List<S3File> call() {
        ListVersionsRequest req = new ListVersionsRequest()
            .withBucketName(_options.getBucketName())
            .withPrefix(_options.getObjectKey().orElse(null));
        if (! _options.isRecursive()) {
          req.setDelimiter("/");
        }
        
        List<S3File> all = new ArrayList<S3File>();
        VersionListing current = getAmazonS3Client().listVersions(req);
        appendVersionSummaryList(all, current.getVersionSummaries());
        if (! _options.dirsExcluded()) {
          appendVersionsDirStringList(all, current.getCommonPrefixes(), _options.getBucketName());
        }
        current = getAmazonS3Client().listNextBatchOfVersions(current);
        
        while (current.isTruncated()) {
          appendVersionSummaryList(all, current.getVersionSummaries());
          if (! _options.dirsExcluded()) {
            appendVersionsDirStringList(all, current.getCommonPrefixes(), _options.getBucketName());
          }
          current = getAmazonS3Client().listNextBatchOfVersions(current);
        }
        appendVersionSummaryList(all, current.getVersionSummaries());
        if (! _options.dirsExcluded()) {
          appendVersionsDirStringList(all, current.getCommonPrefixes(), _options.getBucketName());
        }
        
        return all;
      }
    });
  }
  
  private List<S3File> appendVersionSummaryList(
      List<S3File> all,
      List<S3VersionSummary> appendList) {
    for (S3VersionSummary o : appendList) {
      all.add(versionSummaryToS3File(o));
    }
    
    return all;
  }
  
  private List<S3File> appendVersionsDirStringList(
      List<S3File> all,
      List<String> appendList,
      String bucket) {
    for (String o : appendList) {
      all.add(versionsDirStringToS3File(o, bucket));
    }
    
    return all;
  }
  
  private S3File versionSummaryToS3File(S3VersionSummary o) {
    S3File of = new S3File();
    of.setKey(o.getKey());
    of.setETag(o.getETag());
    of.setBucketName(o.getBucketName());
    if (! o.getVersionId().equals(Constants.NULL_VERSION_ID)) {
      of.setVersionId(o.getVersionId());
    }
    if (o.getLastModified() != null) {
      of.setTimestamp(o.getLastModified());
    }
    of.setSize(o.getSize());
    return of;
  }

  private S3File versionsDirStringToS3File(String dir, String bucket) {
    S3File df = new S3File();
    df.setKey(dir);
    df.setBucketName(bucket);
    return df;
  }
}
