package com.logicblox.s3lib;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.google.api.services.storage.Storage;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;


/**
 * Provides the client for accessing the Google Cloud Storage web service.
 * <p>
 * Captures the full configuration independent of concrete operations like
 * uploads or downloads.
 * <p>
 * For more information about Google Cloud Storage, please see <a
 * href="https://cloud.google.com/storage/">https://cloud.google
 * .com/storage/</a>
 */
public class GCSClient implements CloudStoreClient {
    private static final String GCS_JSON_API_ENDPOINT = "https://www.googleapis.com";
    private static final String GCS_XML_API_ENDPOINT = "https://storage.googleapis.com";


    private final Storage gcsClient;
    private final S3ClientDelegatee s3Client;

    /**
     * @param internalGCSClient Low-level GCS-client
     * @param internalS3Client  Low-level S3-client
     * @param apiExecutor       Executor for executing GCS API calls
     * @param internalExecutor  Executor for internally initiating uploads
     * @param keyProvider       Provider of encryption keys
     */
    GCSClient(
        Storage internalGCSClient,
        AmazonS3Client internalS3Client,
        ListeningExecutorService apiExecutor,
        ListeningScheduledExecutorService internalExecutor,
        KeyProvider keyProvider) {
        s3Client = new S3ClientDelegatee(internalS3Client, apiExecutor,
            internalExecutor, keyProvider);
        gcsClient = internalGCSClient;
        setEndpoint(GCS_XML_API_ENDPOINT);
    }

    /**
     * Canned ACLs handling
     */

    public static final String defaultCannedACL = "projectPrivate";

    public static final List<String> allCannedACLs = Arrays.asList(
        "projectPrivate", "private", "publicRead", "publicReadWrite",
        "authenticatedRead", "bucketOwnerRead", "bucketOwnerFullControl");

    /**
     * {@code cannedACLsDescConst} has to be a compile-time String constant
     * expression. That's why e.g. we cannot re-use {@code allCannedACLs} to
     * construct it.
     */
    static final String cannedACLsDescConst = "For Google Cloud Storage, " +
        "choose one of: projectPrivate, private, publicRead, publicReadWrite," +
        " authenticatedRead,bucketOwnerRead, bucketOwnerFullControl (default:" +
        " projectPrivate).";

    public static boolean isValidCannedACL(String aclStr)
    {
        return allCannedACLs.contains(aclStr);
    }

    @Override
    public void setRetryCount(int retryCount) {
        s3Client.setRetryCount(retryCount);
    }

    @Override
    public void setRetryClientException(boolean retry) {
        s3Client.setRetryClientException(retry);
    }

    @Override
    public void setEndpoint(String endpoint) {
        s3Client.setEndpoint(endpoint);
    }

    @Override
    public String getScheme()
    {
        return "gs";
    }

    @Override
    public ListeningExecutorService getApiExecutor()
    {
        return s3Client.getApiExecutor();
    }

    @Override
    public ListeningScheduledExecutorService getInternalExecutor()
    {
        return s3Client.getInternalExecutor();
    }

    @Override
    public KeyProvider getKeyProvider()
    {
        return s3Client.getKeyProvider();
    }

    @Override
    public ListenableFuture<S3File> upload(UploadOptions options) throws IOException
    {
        return s3Client.upload(options);
    }

    @Override
    public ListenableFuture<List<S3File>> uploadDirectory(UploadOptions options)
        throws IOException,
        ExecutionException, InterruptedException
    {
        return s3Client.uploadDirectory(options);
    }

    @Override
    public ListenableFuture<List<S3File>> deleteDir(DeleteOptions opts)
      throws InterruptedException, ExecutionException
    {
      return s3Client.deleteDir(opts);
    }
    
    @Override
    public ListenableFuture<S3File> delete(DeleteOptions opts)
    {
      return s3Client.delete(opts);
    }

    @Override
    public ListenableFuture<List<Bucket>> listBuckets() {
        return s3Client.listBuckets();
    }

    @Override
    public ListenableFuture<ObjectMetadata> exists(String bucket, String
        object) {
        return s3Client.exists(bucket, object);
    }

    @Override
    public ListenableFuture<S3File> download(DownloadOptions options) throws
        IOException {
        return s3Client.download(options);
    }

    @Override
    public ListenableFuture<List<S3File>> downloadDirectory(DownloadOptions
                                                                    options)
        throws
        IOException, ExecutionException, InterruptedException {
        return s3Client.downloadDirectory(options);
    }

    @Override
    public ListenableFuture<S3File> copy(CopyOptions options)
    {
        return s3Client.copy(options);
    }

    @Override
    public ListenableFuture<List<S3File>> copyToDir(CopyOptions options)
        throws InterruptedException, ExecutionException, IOException
    {
        return s3Client.copyToDir(options);
    }

    @Override
    public ListenableFuture<S3File> rename(RenameOptions options)
    {
        return s3Client.rename(options);
    }

    @Override
    public ListenableFuture<List<S3File>> renameDirectory(RenameOptions options)
      throws InterruptedException, ExecutionException, IOException
    {
        return s3Client.renameDirectory(options);
    }

    @Override
    public ListenableFuture<List<S3File>> listObjects(ListOptions lsOptions) {
        return s3Client.listObjects(lsOptions);
    }

    @Override
    public ListenableFuture<List<Upload>> listPendingUploads(PendingUploadsOptions options) {
        throw new UnsupportedOperationException("listPendingUploads is not " +
            "supported.");
    }

    @Override
    public ListenableFuture<List<Void>> abortPendingUploads(PendingUploadsOptions options) {
        throw new UnsupportedOperationException("abortPendingUpload is not " +
            "supported.");
    }

    @Override
    public ListenableFuture<S3File> addEncryptionKey(EncryptionKeyOptions options)
        throws IOException
    {
        return s3Client.addEncryptionKey(options);
    }

    @Override
    public ListenableFuture<S3File> removeEncryptionKey(EncryptionKeyOptions options)
        throws IOException
    {
        return s3Client.removeEncryptionKey(options);
    }

  @Override
    public void shutdown() {
        s3Client.shutdown();
    }

    private class S3ClientDelegatee extends S3Client {
        public S3ClientDelegatee(
            AmazonS3Client internalS3Client,
            ListeningExecutorService apiExecutor,
            ListeningScheduledExecutorService internalExecutor,
            KeyProvider keyProvider) {
            super(internalS3Client, apiExecutor, internalExecutor, keyProvider);
        }

        void configure(Command cmd) {
            cmd.setRetryClientException(_retryClientException);
            cmd.setRetryCount(_retryCount);
            cmd.setAmazonS3Client(_client);
            cmd.setGCSClient(gcsClient);
            cmd.setScheme("gs://");
        }

        /**
         * Upload file to GCS.
         *
         * @param options Upload options
         */
        @Override
        public ListenableFuture<S3File> upload(UploadOptions options)
            throws IOException {
            GCSUploadCommand cmd = new GCSUploadCommand(options);
            s3Client.configure(cmd);
            return cmd.run();
        }

        /**
         * Upload directory to GCS.
         *
         * @param options Upload options
         */
        @Override
        public ListenableFuture<List<S3File>> uploadDirectory(UploadOptions options)
            throws IOException, ExecutionException, InterruptedException {
            UploadDirectoryCommand cmd = new UploadDirectoryCommand(options);
            s3Client.configure(cmd);
            return cmd.run();
        }

        @Override
        public ListenableFuture<List<S3File>> listObjects(ListOptions options)
        {
          GCSListCommand cmd = new GCSListCommand(options);
          configure(cmd);
          return cmd.run();
        }

        @Override
        public ListenableFuture<S3File> copy(CopyOptions options)
        {
          GCSCopyCommand cmd = new GCSCopyCommand(options);
          configure(cmd);
          return cmd.run();
        }

        @Override
        public ListenableFuture<List<S3File>> copyToDir(CopyOptions options)
          throws IOException
        {
          GCSCopyDirCommand cmd = new GCSCopyDirCommand(options);
          configure(cmd);
          return cmd.run();
        }

        @Override
        protected AddEncryptionKeyCommand createAddKeyCommand(EncryptionKeyOptions options)
            throws IOException
        {
           AddEncryptionKeyCommand cmd = super.createAddKeyCommand(options);
           configure(cmd);
           return cmd;
        }

        @Override
        protected RemoveEncryptionKeyCommand createRemoveKeyCommand(EncryptionKeyOptions options)
            throws IOException
        {
           RemoveEncryptionKeyCommand cmd = super.createRemoveKeyCommand(options);
           configure(cmd);
           return cmd;
        }


    }

    @Override
    public boolean hasBucket(String bucketName)
    {
      throw new UnsupportedOperationException("FIXME - not yet implemented");
    }

    @Override
    public void createBucket(String bucketName)
    {
      throw new UnsupportedOperationException("FIXME - not yet implemented");
    }

    @Override
    public void destroyBucket(String bucketName)
    {
      throw new UnsupportedOperationException("FIXME - not yet implemented");
    }

    // needed for testing
    void setKeyProvider(KeyProvider kp)
    {
      s3Client.setKeyProvider(kp);
    }
}
