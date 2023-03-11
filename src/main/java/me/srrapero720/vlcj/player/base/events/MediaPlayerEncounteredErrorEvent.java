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

package me.srrapero720.vlcj.player.base.events;

import me.srrapero720.vlcj.player.base.MediaPlayer;
import me.srrapero720.vlcj.player.base.MediaPlayerEventListener;

/**
 * Encapsulation of a media player encountered error event.
 */
final class MediaPlayerEncounteredErrorEvent extends MediaPlayerEvent {

    MediaPlayerEncounteredErrorEvent(MediaPlayer mediaPlayer) {
        super(mediaPlayer);
    }

    @Override
    public void notify(MediaPlayerEventListener listener) {
        listener.error(mediaPlayer);
    }

}
