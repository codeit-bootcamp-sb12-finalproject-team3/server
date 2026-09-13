package com.moduplaylist.core.playlist.exception;

public class PlaylistNotFoundException extends RuntimeException {
  public PlaylistNotFoundException(String message) {
    super(message);
  }
}
