package com.aaron.androiddeveloptraining;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final long MB = 1024 * 1024;
    private static final long DISK_CACHE_SIZE = 1024 * 1024 * 5;
    private static final String KEY = "1";

    private ImageView mImageView;
    private ImageCache mImageCache;
    private DiskLruCache mDiskLruCache;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        logMaxMemory();

        mImageCache = RetainFragment.findOrCreateRetainFragment(getSupportFragmentManager()).getImageCache();
        if (mImageCache == null) {
            int cacheSize = (int) (Runtime.getRuntime().maxMemory() / 1024);
            mImageCache = new ImageCache(cacheSize);
            RetainFragment.findOrCreateRetainFragment(getSupportFragmentManager()).setImageCache(mImageCache);
        }

        File cacheFile = getCacheDir();
        new InitDiskCacheTask().execute(cacheFile);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        findViewById(R.id.load_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new BitmapWorkerTask(mImageView).execute("https://avatars2.githubusercontent.com/u/12109585?v=3&s=460");
            }
        });

        mImageView = (ImageView) findViewById(R.id.image);
    }

    private void logMaxMemory() {
        String mm = Runtime.getRuntime().maxMemory() / MB + " Mb";
        Log.d(TAG, "Max runtime memory is " + mm);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    class InitDiskCacheTask extends AsyncTask<File, Void, Void> {
        @Override
        protected Void doInBackground(File... params) {
            try {
                mDiskLruCache = DiskLruCache.open(params[0], 1, 1, DISK_CACHE_SIZE);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap> {
        private final WeakReference<ImageView> mImageViewReference;

        BitmapWorkerTask(ImageView imageView) {
            mImageViewReference = new WeakReference<>(imageView);
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            String urlString = params[0];

            Bitmap bitmap = getBitmapFromCache(KEY);
            if (bitmap != null) {
                return bitmap;
            }

            try {
                DiskLruCache.Editor editor = mDiskLruCache.edit(KEY);
                if (editor != null) {
                    if (fetchImage(urlString, editor.newOutputStream(0))) {
                        editor.commit();
                    } else {
                        editor.abort();
                    }
                }

                mDiskLruCache.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return getBitmapFromCache(KEY);
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);

            if (mImageViewReference != null && bitmap != null) {
                ImageView imageView = mImageViewReference.get();
                imageView.setImageBitmap(bitmap);
            }
        }
    }

    private Bitmap getBitmapFromCache(String key) {
        Bitmap bitmap = mImageCache.get(key);

        if (bitmap != null) {
            Log.d(TAG, "mem cache hit.");
            return bitmap;
        }

        if (mDiskLruCache != null) {
            try {
                DiskLruCache.Snapshot snapshot = mDiskLruCache.get(key);

                if (snapshot != null) {
                    InputStream is = snapshot.getInputStream(0);

                    bitmap = BitmapFactory.decodeStream(is);
                    mImageCache.put(key, bitmap);

                    Log.d(TAG, "disk cache hit.");
                    return bitmap;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private boolean fetchImage(String urlString, OutputStream outputStream) {
        HttpURLConnection connection = null;
        BufferedInputStream in = null;
        BufferedOutputStream out = null;
        try {
            final URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();

            in = new BufferedInputStream(connection.getInputStream());
            out = new BufferedOutputStream(outputStream);
            int line;
            while ((line = in.read()) != -1) {
                out.write(line);
            }

            return true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return false;
    }
}
