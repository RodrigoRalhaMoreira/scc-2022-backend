package scc.cosmosdb.models;

import java.util.Arrays;
import scc.srv.dataclasses.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Represents a User, as stored in the database
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserDAO {
	private String _rid;
	private String _ts;
	private String id;
	private String name;
	private String pwd;
	private String photoId;
	private String[] channelIds;
	private boolean isDeleted;

	public UserDAO() {
	}

	public UserDAO(User u) {
		this(u.getId(), u.getName(), u.getPwd(), u.getPhotoId(), u.getChannelIds());
	}

	public UserDAO(String id, String name, String pwd, String photoId, String[] channelIds) {
		super();
		this.id = id;
		this.name = name;
		this.pwd = pwd;
		this.photoId = photoId;
		this.channelIds = channelIds;
		this.isDeleted = false;
	}

	public String get_rid() {
		return _rid;
	}

	public void set_rid(String _rid) {
		this._rid = _rid;
	}

	public String get_ts() {
		return _ts;
	}

	public void set_ts(String _ts) {
		this._ts = _ts;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPwd() {
		return pwd;
	}

	public void setPwd(String pwd) {
		this.pwd = pwd;
	}

	public String getPhotoId() {
		return photoId;
	}

	public void setPhotoId(String photoId) {
		this.photoId = photoId;
	}

	public String[] getChannelIds() {
		return channelIds == null ? new String[0] : channelIds;
	}

	public void setChannelIds(String[] channelIds) {
		this.channelIds = channelIds;
	}

	public User toUser() {
		return new User(id, name, pwd, photoId,
				channelIds == null ? null : Arrays.copyOf(channelIds, channelIds.length));
	}

	public boolean getDeletedUser() {
		return this.isDeleted;
	}

	public void setDeletedUser() {
		this.isDeleted = true;
	}

	@Override
	public String toString() {
		return "UserDAO [_rid=" + _rid + ", _ts=" + _ts + ", id=" + id + ", name=" + name + ", pwd=" + pwd
				+ ", photoId=" + photoId + ", channelIds=" + Arrays.toString(channelIds) + ", isDeleted="
				+ String.valueOf(isDeleted) + "]";
	}

}
