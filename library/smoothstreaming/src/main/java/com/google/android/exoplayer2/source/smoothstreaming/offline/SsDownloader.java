/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.exoplayer2.source.smoothstreaming.offline;

import android.net.Uri;
import android.util.Pair;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.offline.DownloaderConstructorHelper;
import com.google.android.exoplayer2.offline.SegmentDownloader;
import com.google.android.exoplayer2.source.smoothstreaming.manifest.SsManifest;
import com.google.android.exoplayer2.source.smoothstreaming.manifest.SsManifest.StreamElement;
import com.google.android.exoplayer2.source.smoothstreaming.manifest.SsManifestParser;
import com.google.android.exoplayer2.source.smoothstreaming.manifest.SsUtil;
import com.google.android.exoplayer2.source.smoothstreaming.manifest.TrackKey;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.ParsingLoadable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper class to download SmoothStreaming streams.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * SimpleCache cache = new SimpleCache(downloadFolder, new NoOpCacheEvictor());
 * DefaultHttpDataSourceFactory factory = new DefaultHttpDataSourceFactory("ExoPlayer", null);
 * DownloaderConstructorHelper constructorHelper =
 *     new DownloaderConstructorHelper(cache, factory);
 * SsDownloader ssDownloader = new SsDownloader(manifestUrl, constructorHelper);
 * // Select the first track of the first stream element
 * ssDownloader.selectRepresentations(new TrackKey[] {new TrackKey(0, 0)});
 * ssDownloader.download();
 * // Access downloaded data using CacheDataSource
 * CacheDataSource cacheDataSource =
 *     new CacheDataSource(cache, factory.createDataSource(), CacheDataSource.FLAG_BLOCK_ON_CACHE);
 * }</pre>
 */
public final class SsDownloader extends SegmentDownloader<SsManifest, TrackKey> {

  /**
   * @see SegmentDownloader#SegmentDownloader(Uri, DownloaderConstructorHelper)
   */
  public SsDownloader(Uri manifestUri, DownloaderConstructorHelper constructorHelper)  {
    super(SsUtil.fixManifestUri(manifestUri), constructorHelper);
  }

  @Override
  public TrackKey[] getAllRepresentationKeys() throws IOException {
    ArrayList<TrackKey> keys = new ArrayList<>();
    SsManifest manifest = getManifest();
    for (int i = 0; i < manifest.streamElements.length; i++) {
      StreamElement streamElement = manifest.streamElements[i];
      for (int j = 0; j < streamElement.formats.length; j++) {
        keys.add(new TrackKey(i, j));
      }
    }
    return keys.toArray(new TrackKey[keys.size()]);
  }

  @Override
  protected SsManifest getManifest(DataSource dataSource, Uri uri) throws IOException {
    ParsingLoadable<SsManifest> loadable =
        new ParsingLoadable<>(dataSource, uri, C.DATA_TYPE_MANIFEST, new SsManifestParser());
    loadable.load();
    return loadable.getResult();
  }

  @Override
  protected Pair<List<Segment>, Boolean> getSegments(
      DataSource dataSource, SsManifest manifest, boolean allowIndexLoadErrors) throws IOException {
    ArrayList<Segment> segments = new ArrayList<>();
    for (StreamElement streamElement : manifest.streamElements) {
      for (int i = 0; i < streamElement.formats.length; i++) {
        for (int j = 0; j < streamElement.chunkCount; j++) {
          segments.add(
              new Segment(
                  streamElement.getStartTimeUs(j),
                  new DataSpec(streamElement.buildRequestUri(i, j))));
        }
      }
    }
    return Pair.<List<Segment>, Boolean>create(segments, true);
  }

}
