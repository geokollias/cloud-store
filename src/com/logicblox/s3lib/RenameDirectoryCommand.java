package com.logicblox.s3lib;

import com.google.common.base.Function;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;


public class RenameDirectoryCommand extends Command
{
  private RenameOptions _options;

  public RenameDirectoryCommand(RenameOptions options)
  {
    super(options);
    _options = options;
  }

  public ListenableFuture<List<StoreFile>> run()
    throws InterruptedException, ExecutionException, IOException
  {
    return startCopyThenDelete();
  }

  private ListenableFuture<List<StoreFile>> startCopyThenDelete()
    throws InterruptedException, ExecutionException, IOException
  {
    final String bucket = _options.getDestinationBucketName();
    final String key = stripSlash(_options.getDestinationObjectKey());
       // exists command doesn't allow trailing slash

    ExistsOptions opts = _client.getOptionsBuilderFactory()
      .newExistsOptionsBuilder()
      .setBucketName(bucket)
      .setObjectKey(key)
      .createOptions();

    ListenableFuture<Metadata> destExists = _client.exists(opts);
    return Futures.transform(
      destExists,
      new AsyncFunction<Metadata,List<StoreFile>>()
      {
        public ListenableFuture<List<StoreFile>> apply(Metadata mdata)
          throws Exception
        {
          if(null != mdata)
          {
            throw new UsageException("Cannot overwrite existing destination object '"
              + getUri(bucket, key));
          }
          return copyThenDelete();
        }
      });
  }


  private String stripSlash(String s)
  {
    if(s.endsWith("/"))
      return s.substring(0, s.length() - 1);
    else
      return s;
  }


  private ListenableFuture<List<StoreFile>> copyThenDelete()
    throws InterruptedException, ExecutionException, IOException
  {
    CopyOptions copyOpts = _client.getOptionsBuilderFactory()
       .newCopyOptionsBuilder()
       .setSourceBucketName(_options.getSourceBucketName())
       .setSourceObjectKey(_options.getSourceObjectKey())
       .setDestinationBucketName(_options.getDestinationBucketName())
       .setDestinationObjectKey(_options.getDestinationObjectKey())
       .setRecursive(_options.isRecursive())
       .setDryRun(_options.isDryRun())
       .setCannedAcl(_options.getCannedAcl().orElse(null))
       .createOptions();

    // hack -- exceptions are a bit of a mess.  copyToDir throws all sorts of stuff that 
    //         should be collected into an ExecutionException?
    ListenableFuture<List<StoreFile>> copyFuture = null;
    copyFuture = _client.copyToDir(copyOpts);

    return Futures.transform(
      copyFuture,
      new AsyncFunction<List<StoreFile>, List<StoreFile>>()
      {
        public ListenableFuture<List<StoreFile>> apply(final List<StoreFile> destFiles)
          throws InterruptedException, ExecutionException
        {
          DeleteOptions delOpts = _client.getOptionsBuilderFactory()
            .newDeleteOptionsBuilder()
            .setBucketName(_options.getSourceBucketName())
            .setObjectKey(_options.getSourceObjectKey())
            .setRecursive(_options.isRecursive())
            .setDryRun(_options.isDryRun())
            .createOptions();

          // need to return list of dest files
          return Futures.transform(
            _client.deleteDir(delOpts),
            new Function<List<StoreFile>, List<StoreFile>>()
            {
              public List<StoreFile> apply(List<StoreFile> deletedFiles)
              {
                return destFiles;
              }
            });
        }
      });
  }

}
