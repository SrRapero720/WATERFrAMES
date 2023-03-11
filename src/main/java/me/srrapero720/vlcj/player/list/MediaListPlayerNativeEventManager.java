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

package me.srrapero720.vlcj.player.list;

import me.srrapero720.vlcj.binding.internal.libvlc_event_e;
import me.srrapero720.vlcj.binding.internal.libvlc_event_manager_t;
import me.srrapero720.vlcj.binding.internal.libvlc_event_t;
import me.srrapero720.vlcj.binding.internal.libvlc_instance_t;
import me.srrapero720.vlcj.player.list.events.MediaListPlayerEventFactory;
import me.srrapero720.vlcj.support.eventmanager.EventNotification;
import me.srrapero720.vlcj.support.eventmanager.NativeEventManager;

import static me.srrapero720.vlcj.binding.lib.LibVlc.libvlc_media_list_player_event_manager;

final public class MediaListPlayerNativeEventManager extends NativeEventManager<MediaListPlayer, MediaListPlayerEventListener> {

    MediaListPlayerNativeEventManager(libvlc_instance_t libvlcInstance, MediaListPlayer eventObject) {
        super(libvlcInstance, eventObject, libvlc_event_e.libvlc_MediaListPlayerPlayed, libvlc_event_e.libvlc_MediaListPlayerStopped, "media-list-player-events");
    }

    @Override
    protected libvlc_event_manager_t onGetEventManager(MediaListPlayer eventObject) {
        return libvlc_media_list_player_event_manager(eventObject.mediaListPlayerInstance());
    }

    @Override
    protected EventNotification<MediaListPlayerEventListener> onCreateEvent(libvlc_instance_t libvlcInstance, libvlc_event_t event, MediaListPlayer eventObject) {
        return MediaListPlayerEventFactory.createEvent(libvlcInstance, eventObject, event);
    }

}
