package dream.africa.base.activities;

import dream.africa.base.torrent.TorrentService;

public interface TorrentActivity {

	TorrentService getTorrentService();

	void onTorrentServiceConnected();

	void onTorrentServiceDisconnected();
}
