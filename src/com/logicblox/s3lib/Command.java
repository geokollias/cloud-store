package com.logicblox.s3lib;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.IOException;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.security.Key;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

public class Command
{
  protected int _retryCount = 50;
  protected File file;
  protected long chunkSize;
  protected Key encKey;
  protected long fileLength;

  public void setRetryCount(int retryCount)
  {
    _retryCount = retryCount;
  }

  protected static Key readKeyFromFile(String encKeyName, File encKeyFile) throws IOException, ClassNotFoundException
  {
    FileInputStream fs = new FileInputStream(encKeyFile);
    ObjectInputStream in = new ObjectInputStream(fs);
    Map<String,Key> keys = (HashMap<String,Key>) in.readObject();
    in.close();
    if (keys.containsKey(encKeyName)) {
      return keys.get(encKeyName);
    }
    return null;
  }

  protected <V> ListenableFuture<V> withFallback(ListenableFuture<V> future, FutureFallback<V> fallback)
  {
    return new FallbackFuture<V>(future, fallback, MoreExecutors.sameThreadExecutor());
  }

  /**
   * Utility to rethrow the given throwable as an exception if the
   * retryCount has exceeded the configured maximum.
   */
  protected void rethrowOnMaxRetry(Throwable thrown, int retryCount) throws Exception
  {
    if(retryCount > _retryCount)
    {
      if(thrown instanceof Exception)
        throw (Exception) thrown;
      if(thrown instanceof Error)
        throw (Error) thrown;
      else
        throw new RuntimeException(thrown);
    }

    /*
     if(thrown instanceof AmazonServiceException) {
       AmazonServiceException exc = (AmazonServiceException) thrown;
       if(exc.getErrorType() == ErrorType.Client)
         throw exc;
     }
     */
  }

}