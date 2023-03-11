/*
 * This file is part of VLCJ.
 *
 * VLCJ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * VLCJ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with VLCJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2009-2022 Caprica Software Limited.
 */

package me.srrapero720.vlcj.media;

import me.srrapero720.vlcj.binding.internal.libvlc_media_tracklist_t;
import me.srrapero720.vlcj.player.base.AudioTrackList;
import me.srrapero720.vlcj.player.base.TextTrackList;
import me.srrapero720.vlcj.player.base.TrackList;
import me.srrapero720.vlcj.player.base.VideoTrackList;

import static me.srrapero720.vlcj.binding.lib.LibVlc.libvlc_media_get_tracklist;

/**
 * Behaviour pertaining to media tracks, providing things like track information.
 */
public final class TrackApi extends BaseApi {

    TrackApi(Media media) {
        super(media);
    }

    /**
     * Get the media video track list for the given track type.
     * <p>
     * The returned track list must be freed by {@link TrackList#release()} when it is no longer needed.
     *
     * @return track list, or <code>null</code> if no track list for the requested type is available
     */
    public VideoTrackList videoTracks() {
        libvlc_media_tracklist_t trackList = libvlc_media_get_tracklist(mediaInstance, TrackType.VIDEO.intValue());
        if (trackList != null) {
            return new VideoTrackList(trackList);
        }
        return null;
    }

    /**
     * Get the media audio track list for the given track type.
     * <p>
     * The returned track list must be freed by {@link TrackList#release()} when it is no longer needed.
     *
     * @return track list, or <code>null</code> if no track list for the requested type is available
     */
    public AudioTrackList audioTracks() {
        libvlc_media_tracklist_t trackList = libvlc_media_get_tracklist(mediaInstance, TrackType.AUDIO.intValue());
        if (trackList != null) {
            return new AudioTrackList(trackList);
        }
        return null;
    }

    /**
     * Get the media audio track list for the given track type.
     * <p>
     * The returned track list must be freed by {@link TrackList#release()} when it is no longer needed.
     *
     * @return track list, or <code>null</code> if no track list for the requested type is available
     */
    public TextTrackList textTracks() {
        libvlc_media_tracklist_t trackList = libvlc_media_get_tracklist(mediaInstance, TrackType.TEXT.intValue());
        if (trackList != null) {
            return new TextTrackList(trackList);
        }
        return null;
    }
}
