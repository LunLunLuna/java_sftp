package com.zenithst.common.util.sftp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.zenithst.common.util.ZappFileUtil;

/**
 * <pre>
 * SFTP connection 처리 공통 유틸
 *
 * @author  ZenithST
 * @since   2020.02.19
 * @version 1.0
 * </pre>
 */
public class SFTPClient
{
	private static final Logger logger = LoggerFactory.getLogger(SFTPClient.class);
	
	private Session session = null;
	private Channel channel = null;
	private ChannelSftp channelSftp = null;
	
	
	/**
     * <pre>
     * SFTP 서버 접속
     * @param host 서버 주소
     * @param port 접속 포트 번호
     * @param username 사용자 ID
     * @param password 비밀번호
     * @return boolean
     * </pre>
     */
	public boolean connect(String host, int port, String username, String password) {
		boolean result = false;
		JSch jsch = new JSch();
		try {
			// session 객체 생성
			session = jsch.getSession(username, host, port);
			session.setPassword(password);
			
			// session config 설정 
			java.util.Properties config = new java.util.Properties();
			config.put("StrictHostKeyChecking", "no"); // 호스트 정보 검사하지 않음
			session.setConfig(config);
			
			// session 접속
			session.connect();
			
			// sftp channel 접속
			channel = session.openChannel("sftp");
			channel.connect();
			
			// channel을 FTP용 채널 객체로 캐스팅
			channelSftp = (ChannelSftp) channel;
			
			result = true;
			logger.debug("SFTPClient - Connection sucess... ");
		}
		catch (JSchException e) {
			logger.error("SFTPClient - Connection fail... [" + host + ", " + port + "] JSchException : " + e.toString());
			result = false;
		}
		return result;
	}
	
	
	/**
     * <pre>
     * SFTP 연결 해제
     * </pre>
     */
	public void disconnect() {
		try {
			if (session.isConnected()) {
				channelSftp.disconnect();
				channel.disconnect();
				session.disconnect();
				logger.debug("SFTPClient - disconnect sucess... ");
			}
			//channelSftp.quit();
			//session.disconnect();
		}
		catch (Exception e) {
			logger.error("SFTPClient - disconnect() Exception : " + e.toString());
		}
	}
	
	
	/**
     * <pre>
     * 접속한 계정의 홈 디렉토리 경로 조회
     * @return String
     * </pre>
     */
    public String getHomeDir() {
    	String homeDir = "";
    	try {
    		homeDir = channelSftp.getHome();
    	}
    	catch (Exception e) {
			logger.error("SFTPClient - getHomeDir() Exception : " + e.toString());
		}
    	logger.debug("SFTPClient - getHomeDir() : " + homeDir);
    	return homeDir;
    }
    
	
    /**
     * <pre>
     * 서버의 디렉토리 존재 여부 체크
     * @param remoteDir 서버 디렉토리 경로
     * @return boolean
     * </pre>
     */
    public boolean existsDir(String remoteDir) {
    	boolean result = false;
    	try {
    		channelSftp.ls(remoteDir);
    		result = true;
    	}
    	catch (Exception e) {
			//logger.error("SFTPClient - existsDir() Exception : " + e.toString());
			result = false;
		}
    	//logger.debug("SFTPClient - existsDir [" + remoteDir + "] : " + result);
    	return result;
    }
    
    
    /**
     * <pre>
     * 서버에 폴더 생성
     * @param remoteDir 서버에 생성할 디렉토리 경로
     * @return boolean
     * </pre>
     */
	public boolean mkdir(String remoteDir) {
    	boolean result = false;
    	try {
    		remoteDir = ZappFileUtil.checkPathString(remoteDir); // 마지막에 / 추가됨
    		
    		String[] dirTemp = remoteDir.split("/");
    		String remoteDirTemp = "";
    		
    		for (int i=0; i<dirTemp.length; i++) {
    			//logger.debug("dirTemp["+i+"] : " + dirTemp[i]);
    			remoteDirTemp = remoteDirTemp + dirTemp[i] + "/";
    			if (i > 0) {
    				if (existsDir(remoteDirTemp)) {
        				channelSftp.cd(remoteDirTemp);
        				//logger.debug("dirTemp["+i+"] => cd " + remoteDirTemp);
        			}
        			else {
        				channelSftp.mkdir(remoteDirTemp);
        				//logger.debug("dirTemp["+i+"] => mkdir " + remoteDirTemp);
        			}
    			}
    		}
    		
    		if (existsDir(remoteDir)) {
    			result = true;
    		}
    		else {
    			result = false;
    		}
    	}
    	catch (Exception e) {
			logger.error("SFTPClient - existsDir() Exception : " + e.toString());
			result = false;
		}
    	//logger.debug("SFTPClient - mkdir [" + remoteDir + "] : " + result);
    	return result;
    }
    
    
    /**
     * <pre>
     * 파일 업로드
     * @param remoteDir      서버 디렉토리 경로
     * @param localFilePath  업로드할 로컬 파일 경로
     * @return boolean
     * </pre>
     */
	public boolean upload(String remoteDir, String localFilePath) throws SftpException, FileNotFoundException, Exception {
		boolean result = false;
		FileInputStream in = null;
		try {
			//if (!existsDir(remoteDir)) {
			//	if (!mkdir(remoteDir)) {
			//		return result;
			//	}
			//}
			
			File file = new File(localFilePath);
			in = new FileInputStream(file);
			
			channelSftp.cd(remoteDir);
			channelSftp.put(in, file.getName());
			
			result = true;
		} 
		catch (SftpException e) {
			result = false;
			logger.error("SFTPClient - upload() : " + localFilePath + " -> " + remoteDir + " SftpException: " + e.toString());
		} 
		catch (FileNotFoundException e) {
			result = false;
			logger.error("SFTPClient - upload() : " + localFilePath + " -> " + remoteDir + " FileNotFoundException: " + e.toString());
		} 
		catch (Exception e) {
			result = false;
			logger.error("SFTPClient - upload() : " + localFilePath + " -> " + remoteDir + " Exception: " + e.toString());
		}
		finally {
			try { in.close(); } catch (Exception e) { logger.error(e.toString()); }
		}
		
		return result;
	}
	

	/**
     * <pre>
     * 파일 다운로드
     * @param remoteDir      서버 디렉토리 경로
     * @param remoteFileName 서버 파일명
     * @param localFilePath  다운로드 후 로컬에 저장할 파일 경로
     * @return boolean
     * </pre>
     */
	public boolean download(String remoteDir, String remoteFileName, String localFilePath) {
		boolean result = false;
		InputStream in = null;
		FileOutputStream out = null;
		
		try {
			
			channelSftp.cd(remoteDir);
			
			in = channelSftp.get(remoteFileName);
			out = new FileOutputStream(new File(localFilePath));
			
			int i;
			while ((i = in.read()) != -1) {
				out.write(i);
			}
			
			result = true;
		}
		catch (IOException e) {
			result = false;
			logger.error("SFTPClient - download() : " + remoteDir + remoteFileName + " -> " + localFilePath + " IOException: " + e.toString());
		}
		catch (Exception e) {
			result = false;
			logger.error("SFTPClient - download() : " + remoteDir + remoteFileName + " -> " + localFilePath + " Exception: " + e.toString());
		}
		finally {
			try { out.close(); } catch (Exception e) { logger.error(e.toString()); }
			try { in.close(); } catch (Exception e) { logger.error(e.toString()); }
		}
		
		return result;
	}
	
	
	/**
     * <pre>
     * 서버의 파일 목록 조회
     * @param remoteDir 서버 디렉토리 경로
     * @param remoteFileName 삭제할 파일명
     * @param ext 삭제할 파일 확장자
     * @return void
     * </pre>
     */
	public ArrayList<String> getFileList(String remoteDir, String ext) {
		ArrayList<String> fileList = new ArrayList<String>();
		try {
			Vector<ChannelSftp.LsEntry> fileAndFolderList = channelSftp.ls(remoteDir);
			logger.debug("SFTPClient - getFileList() remoteDir : " + remoteDir);
			//logger.debug("SFTPClient - getFileList() fileAndFolderList.size() : " + fileAndFolderList.size());
			
			String lsFileName = "";
			for (ChannelSftp.LsEntry item : fileAndFolderList) {
				//logger.debug("SFTPClient - getFileList() item.getFilename() : " + item.getFilename());
				
				lsFileName = item.getFilename();
				if (".".equals(lsFileName) || "..".equals(lsFileName)) {
					continue;
				}
				else if (!"".equals(ext) && lsFileName.indexOf(ext) < 0) {
					continue;
				}
				
				fileList.add(lsFileName);
				//logger.debug("SFTPClient - getFileList() lsFileName : " + lsFileName);
			}
		}
		catch (Exception e) {
			logger.error("SFTPClient - getFileList() Exception : " + e.toString());
		}
		//logger.debug("SFTPClient - getFileList() fileList.size() : " + fileList.size());
		return fileList;
	}
	
	
	/**
     * <pre>
     * 서버의 파일 삭제
     * @param remoteDir      서버 디렉토리 경로
     * @param remoteFileName 서버 파일명
     * @return boolean
     * </pre>
     */
	public boolean removeFile(String remoteDir, String remoteFileName) {
		boolean result = false;
		try {
			remoteDir = ZappFileUtil.checkPathString(remoteDir); // 마지막에 / 추가
			channelSftp.rm(remoteDir + remoteFileName);
			result = true;
		}
		catch (Exception e) {
			result = false;
			logger.error("SFTPClient - removeFile() : " + remoteDir + remoteFileName + " Exception: " + e.toString());
		}
		return result;
	}
	
	
	/**
     * <pre>
     * 서버의 파일 삭제 (다건 일괄 삭제)
     * @param fileList ArrayList 삭제할 파일 리스트
     * @return int
     * </pre>
     */
	public int removeFiles(ArrayList<String> fileList) {
		int delCnt = 0;
		try {
			String filePath = "";
			for (int i=0; i<fileList.size(); i++) {
				filePath = fileList.get(i);
				try {
					channelSftp.rm(filePath);
					delCnt ++;
				}
				catch (Exception e) {
					logger.error("SFTPClient - removeFiles() : " + filePath + " Exception: " + e.toString());
				}
			}
		}
		catch (Exception e) {
			logger.error("SFTPClient - removeFiles() Exception: " + e.toString());
		}
		return delCnt;
	}
	
}

