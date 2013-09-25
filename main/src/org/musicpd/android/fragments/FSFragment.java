package org.musicpd.android.fragments;

import java.util.ArrayList;
import java.util.List;

import org.a0z.mpd.Directory;
import org.a0z.mpd.Item;
import org.a0z.mpd.MPDCommand;
import org.a0z.mpd.Music;
import org.a0z.mpd.exception.MPDServerException;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.AdapterView;

import org.musicpd.android.R;
import org.musicpd.android.library.ILibraryFragmentActivity;
import org.musicpd.android.tools.Log;
import org.musicpd.android.tools.Tools;

public class FSFragment extends BrowseFragment {
	private static final String EXTRA_DIRECTORY = "directory";

	private Directory currentDirectory = null;
	private String directory = null;

	public FSFragment() {
		super(R.string.addDirectory, R.string.addedDirectoryToPlaylist, MPDCommand.MPD_SEARCH_FILENAME);
	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		if (icicle != null)
			init(icicle.getString(EXTRA_DIRECTORY));
	}

	@Override
	public String getTitle() {
		if(directory == null) {
			return getString(R.string.files);
		} else {
			return directory;
		}
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putString(EXTRA_DIRECTORY, directory);
		super.onSaveInstanceState(outState);
	}

	public FSFragment init(String path) {
		directory = path;
		return this;
	}
	
	@Override
	protected void add(Item item, boolean replace, boolean play) {
		try {
			final Directory ToAdd = currentDirectory.getDirectory(item.getName());
			if (ToAdd != null) {
				// Valid directory
				app.oMPDAsyncHelper.oMPD.add(ToAdd, replace, play);
				Tools.notifyUser(String.format(getResources().getString(R.string.addedDirectoryToPlaylist), item),
						FSFragment.this.getActivity());
			} else {
				app.oMPDAsyncHelper.oMPD.add((Music) item, replace, play);
				Tools.notifyUser(getResources().getString(R.string.songAdded, item), FSFragment.this.getActivity());
			}
		} catch (Exception e) {
			Log.w(e);
		}
	}
	
	@Override
	protected void add(Item item, String playlist) {
		try {
			Directory ToAdd = currentDirectory.getDirectory(item.getName());
			if (ToAdd != null) {
				// Valid directory
				app.oMPDAsyncHelper.oMPD.addToPlaylist(playlist, ToAdd);
				Tools.notifyUser(String.format(getResources().getString(R.string.addedDirectoryToPlaylist), item),
						FSFragment.this.getActivity());
			} else {
				ArrayList<Music> songs = new ArrayList<Music>();
				songs.add((Music) item);
				app.oMPDAsyncHelper.oMPD.addToPlaylist(playlist, songs);
				Tools.notifyUser(getResources().getString(R.string.songAdded, item), FSFragment.this.getActivity());
			}
		} catch (Exception e) {
			Log.w(e);
		}
	}
	
	@Override
	protected void asyncUpdate() {
		if (directory != null) {
			currentDirectory = app.oMPDAsyncHelper.oMPD.getRootDirectory().makeDirectory(directory);
		} else {
			currentDirectory = app.oMPDAsyncHelper.oMPD.getRootDirectory();
		}

		try {
			currentDirectory.refreshData();
		} catch (MPDServerException e) {
			Log.w(e);
		}

		List<Item> dirItems=new ArrayList<Item>();
		dirItems.addAll(currentDirectory.getDirectories());
		dirItems.addAll(currentDirectory.getFiles());
		items=dirItems;
	}

	@Override
	public void onItemClick(AdapterView l, View v, int position, long id) {
		final Item item = items.get(position);
		// click on a file
		if (Music.class.isInstance(item)) {

			final Music music = (Music) item;
			app.oMPDAsyncHelper.execAsync(new Runnable() {
				@Override
				public void run() {
					try {
						int songId = -1;
						app.oMPDAsyncHelper.oMPD.getPlaylist().add(music);
						if (songId > -1) {
							app.oMPDAsyncHelper.oMPD.skipToId(songId);
						}
					} catch (MPDServerException e) {
						Log.w(e);
					}
				}
			});
		} else {
			final String dir = ((Directory) item).getFullpath();
			((ILibraryFragmentActivity) getActivity()).pushLibraryFragment(new FSFragment().init(dir), "filesystem");
		}

	}
	
	//Disable the indexer for FSFragment
	@SuppressWarnings("unchecked")
	protected ListAdapter getCustomListAdapter() {
		return new ArrayAdapter<Item>(getActivity(), R.layout.simple_list_item_1, (List<Item>) items);
	}

}