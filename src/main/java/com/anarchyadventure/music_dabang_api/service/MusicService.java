package com.anarchyadventure.music_dabang_api.service;

import com.anarchyadventure.music_dabang_api.dto.common.PageRequest;
import com.anarchyadventure.music_dabang_api.dto.common.PageResponse;
import com.anarchyadventure.music_dabang_api.dto.music.MusicContentDTO;
import com.anarchyadventure.music_dabang_api.dto.music.playlist.PlaylistDTO;
import com.anarchyadventure.music_dabang_api.dto.music.playlist.PlaylistItemDTO;
import com.anarchyadventure.music_dabang_api.entity.music.MusicContent;
import com.anarchyadventure.music_dabang_api.entity.music.MusicContentType;
import com.anarchyadventure.music_dabang_api.entity.music.Playlist;
import com.anarchyadventure.music_dabang_api.entity.music.PlaylistItem;
import com.anarchyadventure.music_dabang_api.entity.user.User;
import com.anarchyadventure.music_dabang_api.repository.music.MusicContentRepository;
import com.anarchyadventure.music_dabang_api.repository.music.PlaylistItemRepository;
import com.anarchyadventure.music_dabang_api.repository.music.PlaylistRepository;
import com.anarchyadventure.music_dabang_api.security.SecurityHandler;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class MusicService {
    private final MusicContentRepository musicContentRepository;
    private final PlaylistRepository playlistRepository;
    private final PlaylistItemRepository playlistItemRepository;

    @Transactional(readOnly = true)
    public PageResponse<PlaylistDTO> findAllPlaylists(PageRequest pageRequest) {
        List<Playlist> playlists = playlistRepository.paginateMainPlaylist(pageRequest);
        List<PlaylistDTO> data = playlists.stream()
            .map(PlaylistDTO::from)
            .toList();
        return PageResponse.<PlaylistDTO>builder()
            .data(data)
            .cursor(data.isEmpty() ? null : data.get(data.size() - 1).getId().toString())
            .build();
    }

    @Transactional(readOnly = true)
    public PageResponse<PlaylistItemDTO> findAllMyPlaylistItems(PageRequest pageRequest) {
        List<PlaylistItem> items = playlistItemRepository
            .paginateMyPlaylistItems(SecurityHandler.getUserAuth().getId(), pageRequest);
        List<PlaylistItemDTO> data = items.stream()
            .map(PlaylistItemDTO::from)
            .toList();
        return PageResponse.<PlaylistItemDTO>builder()
            .data(data)
            .cursor(data.isEmpty() ? null : data.get(data.size() - 1).getId().toString())
            .build();
    }

    public PlaylistItemDTO addMyPlaylistItem(Long musicId) {
        MusicContent music = musicContentRepository.findByIdOpt(musicId)
            .orElseThrow(() -> new EntityNotFoundException("Music not found"));
        PlaylistItem item = new PlaylistItem(SecurityHandler.getUserAuth(), music, null);
        playlistItemRepository.save(item);
        return PlaylistItemDTO.from(item);
    }

    public void deletePlaylistItem(List<Long> itemIds) {
        playlistItemRepository.deleteAllByItemIdsAndUserId(itemIds, SecurityHandler.getUserAuth().getId());
    }

    @Transactional(readOnly = true)
    public PageResponse<PlaylistItemDTO> findPlaylistItems(Long playlistId, PageRequest pageRequest) {
        List<PlaylistItem> items = playlistItemRepository
            .paginatePlaylistItems(SecurityHandler.getUserAuth().getId(), playlistId, pageRequest);
        List<PlaylistItemDTO> data = items.stream()
            .map(PlaylistItemDTO::from)
            .toList();
        return PageResponse.<PlaylistItemDTO>builder()
            .data(data)
            .cursor(data.isEmpty() ? null : data.get(data.size() - 1).getId().toString())
            .build();
    }

    public PlaylistItemDTO addPlaylistItem(Long playlistId, Long musicContentId) {
        User user = SecurityHandler.getUserAuth();
        MusicContent musicContent = musicContentRepository.findByIdOpt(musicContentId)
            .orElseThrow(() -> new EntityNotFoundException("음악 정보를 찾을 수 없습니다."));
        Playlist playlist = playlistRepository.findByUserIdAndPlaylistId(user.getId(), playlistId)
            .orElseThrow(() -> new EntityNotFoundException("플레이 리스트를 찾을 수 없습니다."));
        PlaylistItem item = new PlaylistItem(user, musicContent, playlist);
        playlistItemRepository.save(item);
        return PlaylistItemDTO.from(item);
    }

    @Transactional(readOnly = true)
    public PageResponse<MusicContentDTO> searchMusic(String query, MusicContentType musicContentType, PageRequest pageRequest) {
        List<MusicContent> musicContents = musicContentRepository
            .search(query, musicContentType, pageRequest);
        List<MusicContentDTO> data = musicContents.stream()
            .map(MusicContentDTO::from)
            .toList();
        return PageResponse.<MusicContentDTO>builder()
            .data(data)
            .cursor(data.isEmpty() ? null : data.get(data.size() - 1).getId().toString())
            .build();
    }

    @Transactional(readOnly = true)
    public List<String> autoCompleteSearchKeyword(String query, Integer limit) {
        return musicContentRepository.autoComplete(query, limit);
    }
}
