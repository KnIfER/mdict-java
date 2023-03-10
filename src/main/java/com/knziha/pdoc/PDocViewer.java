package com.knziha.pdoc;

import com.knziha.plod.dictionary.Utils.*;

import java.io.*;
import java.nio.charset.StandardCharsets;

import static com.knziha.pdoc.Numbers.*;
import static com.knziha.pdoc.Numbers.PDF_DATAAVAIL_STATUS.*;

public class PDocViewer {
	static  class TestLoader {
		// 构造
		TestLoader(byte[] pBuf, int len) {
			m_pBuf=pBuf;
			m_Len=len;
		}
		byte[] m_pBuf;
		int m_Len;
	}
	
	static  class FPDF_FILEACCESS {
		TestLoader m_Param;
		long	m_FileLen;
		int ReadBlock(byte[] pBuf, long pos, long size) { // Get_Block
			//TestLoader pLoader = (TestLoader) param;
			if (pos + size < pos || pos + size > m_Param.m_Len) return 0;
			System.arraycopy(m_Param.m_pBuf, (int)pos, pBuf, 0, (int)size);
			return 1;
		}

		long GetSize()
		{
			return m_FileLen;
		}
	}
	
	static class FX_FILEAVAIL {
		int version;
		boolean IsDataAvail(int offset, int size) { // Is_Data_Avail
			return true;
		}
	}

	static class FX_DOWNLOADHINTS {
		int version;
		void Add_Segment(FX_DOWNLOADHINTS pThis, int offset, int size) {
		}
	}
	static class CPDF_DataAvail
	{
		FX_FILEAVAIL		m_pFileAvail;
		FPDF_FILEACCESS			m_pFileRead;

		public CPDF_DataAvail(FX_FILEAVAIL file_avail, FPDF_FILEACCESS file_access) {
			m_pFileAvail = file_avail;
			m_pFileRead = file_access;
		}

		public int IsLinearizedPDF() throws IOException {
			int req_size = 1024;
			if (!m_pFileAvail.IsDataAvail(0, req_size)) {
				return PDF_UNKNOW_LINEARIZED;
			}
			if (m_pFileRead==null) {
				return PDF_NOT_LINEARIZED;
			}
			long dwSize = m_pFileRead.GetSize();
			if (dwSize < (long)req_size) {
				return PDF_UNKNOW_LINEARIZED;
			}
			byte[] buffer = new byte[1024];
			m_pFileRead.ReadBlock(buffer, 0, req_size);
			if (IsLinearizedFile(buffer, req_size)) {
				return PDF_IS_LINEARIZED;
			}
			return PDF_NOT_LINEARIZED;
		}

		static long GetHeaderOffset(BSIFile pFile) throws IOException {
			int tag = 0x46445025;
			byte[] buf = new byte[4];
			long offset = 0;
			while (true) {
				pFile.setPosition((int) offset);
				if (pFile.read(buf, 0, 4)==0) {
					return -1;
				}
				if (BU.toIntLE(buf, 0) == tag) {
					return offset;
				}
				offset ++;
				if (offset > 1024) {
					return -1;
				}
			}
		}

		PDF_DATAAVAIL_STATUS	m_docStatus;
		CPDF_SyntaxParser		m_syntaxParser = new CPDF_SyntaxParser();

		int m_dwHeaderOffset;
		int m_dwLastXRefOffset;
		int m_dwXRefOffset;
		int m_dwTrailerOffset;
		int m_dwCurrentOffset;
		
		boolean IsLinearizedFile(byte[] pData, long dwLen) throws IOException {
			BSIFile file = new BSIFile(pData, 0, (int)dwLen);
			long offset = GetHeaderOffset(file);
			//CMN.Log("offset::", offset, mdict.indexOf(pData, 0, 1024, new byte[]{0x46, 0x44, 0x50, 0x25}, 0, 4, 0));
			//CMN.Log("offset::", offset, mdict.indexOf(pData, 0, 1024, new byte[]{0x25, 0x50, 0x44, 0x46}, 0, 4, 0));
			if (offset == -1) {
				m_docStatus = PDF_DATAAVAIL_ERROR;
				return false;
			}
			m_dwHeaderOffset = (int) offset;
			m_syntaxParser.InitParser(file, (int) offset);
			m_syntaxParser.RestorePos(m_syntaxParser.m_HeaderOffset + 9);
			blag bNumber = new blag();
			String wordObjNum = m_syntaxParser.GetNextWord(bNumber);
			//CMN.Log("wordObjNum", wordObjNum, bNumber);
			if (!bNumber.val) {
				return false;
			}
			int objnum = IU.parseInt(wordObjNum);
			
			return false;
		}
	}

	static class CPDF_SyntaxParser{
		CPDF_SyntaxParser() {
			m_pFileAccess = null;
			//m_pCryptoHandler = NULL;
			m_pFileBuf = null;
			m_BufSize = CPDF_ModuleMgr_m_FileBufSize;
			m_pFileBuf = null;
			m_MetadataObjnum = 0;
			//m_dwWordPos = 0;
			m_bFileStream = false;
		}
		int MAX_WORD_BUFFER=256;
		boolean GetNextChar(clag ch)
		{
			long pos = m_Pos + m_HeaderOffset;
			if (pos >= m_FileLen) {
				return false;
			}
			if (m_BufOffset >= pos || (long)(m_BufOffset + m_BufSize) <= pos) {
				long read_pos = pos;
				int read_size = m_BufSize;
				if ((long)read_size > m_FileLen) {
					read_size = (int)m_FileLen;
				}
				if ((long)(read_pos + read_size) > m_FileLen) {
					if (m_FileLen < (long)read_size) {
						read_pos = 0;
						read_size = (int)m_FileLen;
					} else {
						read_pos = m_FileLen - read_size;
					}
				}
				//CMN.Log("read read ", read_pos, read_size);
				m_pFileAccess.setPosition((int) read_pos);
				if (m_pFileAccess.read(m_pFileBuf, 0, read_size)==0) {
					return false;
				}
				m_BufOffset = read_pos;
			}
			ch.val = (char) (0xff&m_pFileBuf[(int) (pos - m_BufOffset)]);
			//CMN.Log("read read ", (int) (pos - m_BufOffset), Integer.toHexString(ch.val), Integer.toHexString(m_pFileBuf[0]));
			m_Pos ++;
			return true;
		}
		void GetNextWord()
		{
			m_WordSize = 0;
			m_bIsNumber = true;
			clag ch = new clag();
			if (!GetNextChar(ch)) {
				return;
			}
			char type = _PDF_CharType[ch.val&0xff];
			//CMN.Log(ch, type);
			while (true) {
				while (type == 'W') {
					if (!GetNextChar(ch)) {
						return;
					}
					type = _PDF_CharType[ch.val&0xff];
				}
				if (ch.val != '%') {
					break;
				}
				//CMN.Log("in in ");
				while (true) {
					if (!GetNextChar(ch)) {
						return;
					}
					if (ch.val == '\r' || ch.val == '\n') {
						break;
					}
				}
				type = _PDF_CharType[ch.val&0xff];
			}
			if (type == 'D') {
				m_bIsNumber = false;
				m_WordBuffer[m_WordSize++] = (byte) ch.val;
				if (ch.val == '/') {
					while (true) {
						if (!GetNextChar(ch)) {
							return;
						}
						type = _PDF_CharType[ch.val&0xff];
						if (type != 'R' && type != 'N') {
							m_Pos --;
							return;
						}
						if (m_WordSize < MAX_WORD_BUFFER) {
							m_WordBuffer[m_WordSize++] = (byte) ch.val;
						}
					}
				} else if (ch.val == '<') {
					if (!GetNextChar(ch)) {
						return;
					}
					if (ch.val == '<') {
						m_WordBuffer[m_WordSize++] = (byte) ch.val;
					} else {
						m_Pos --;
					}
				} else if (ch.val == '>') {
					if (!GetNextChar(ch)) {
						return;
					}
					if (ch.val == '>') {
						m_WordBuffer[m_WordSize++] = (byte) ch.val;
					} else {
						m_Pos --;
					}
				}
				return;
			}
			while (true) {
				if (m_WordSize < MAX_WORD_BUFFER) {
					m_WordBuffer[m_WordSize++] = (byte) ch.val;
				}
				if (type != 'N') {
					m_bIsNumber = false;
				}
				if (!GetNextChar(ch)) {
					return;
				}
				type = _PDF_CharType[ch.val&0xff];
				if (type == 'D' || type == 'W') {
					m_Pos --;
					break;
				}
			}
		}
		String GetNextWord(blag bIsNumber)
		{
			GetNextWord();
			bIsNumber.val = m_bIsNumber;
			return new String(m_WordBuffer, 0, m_WordSize, StandardCharsets.UTF_8);
		}
		void RestorePos(long pos)
		{
			m_Pos = pos;
		}
		void InitParser(BSIFile pFileAccess, int HeaderOffset) {
			if (m_pFileBuf!=null) {
				m_pFileBuf = null;
			}
			m_pFileBuf = new byte[m_BufSize];
			m_HeaderOffset = HeaderOffset;
			m_FileLen = pFileAccess.available();
			m_Pos = 0;
			m_pFileAccess = pFileAccess;
			m_BufOffset = 0;
			pFileAccess.setPosition(0);
			pFileAccess.read(m_pFileBuf, 0, (int)((long)m_BufSize > m_FileLen ? m_FileLen : m_BufSize));
		}

		long			m_Pos;
		boolean				m_bFileStream;
		int					m_MetadataObjnum;
		BSIFile		m_pFileAccess;
		int			m_HeaderOffset;
		long			m_FileLen;
		byte[]			m_pFileBuf;
		int			m_BufSize;
		long			m_BufOffset;
		//CPDF_CryptoHandler*	m_pCryptoHandler;
		byte[]			m_WordBuffer = new byte[257];
		int				m_WordSize;
		boolean				m_bIsNumber;
	}
	
	static class CFPDF_DataAvail
	{
//		CFPDF_DataAvail()
//		{
//			m_pDataAvail = NULL;
//		}

//		~CFPDF_DataAvail()
//		{
//			if (m_pDataAvail) delete m_pDataAvail;
//		}
		CPDF_DataAvail			m_pDataAvail;
		FX_FILEAVAIL	m_FileAvail;
		FPDF_FILEACCESS	m_FileRead;

		public CFPDF_DataAvail(FX_FILEAVAIL file_avail, FPDF_FILEACCESS file_access) {
			m_FileAvail = file_avail;
			m_FileRead = file_access;
			m_pDataAvail = new CPDF_DataAvail(file_avail, file_access);
		}
	};
	
	
	
	
	
	
	
	
	
	public static void main(String[] args) throws IOException {
		File f = new File("D:\\PDFJsAnnot\\1.pdf");
		int len=(int) f.length();
		byte[] pBuf = new byte[len];
		FileInputStream fin = new FileInputStream(f);
		fin.read(pBuf);
		
		TestLoader loader = new TestLoader(pBuf, len);

		FPDF_FILEACCESS file_access = new FPDF_FILEACCESS();
		file_access.m_FileLen = len;
		file_access.m_Param = loader;


		FX_FILEAVAIL file_avail = new FX_FILEAVAIL();
		file_avail.version = 1;

		FX_DOWNLOADHINTS hints = new FX_DOWNLOADHINTS();
		hints.version = 1;

		CFPDF_DataAvail pdf_avail = new CFPDF_DataAvail(file_avail, file_access);

		pdf_avail.m_pDataAvail.IsLinearizedPDF();
		
	}
	
	
	
	
	
	
	
	
	
	
	
	
}
