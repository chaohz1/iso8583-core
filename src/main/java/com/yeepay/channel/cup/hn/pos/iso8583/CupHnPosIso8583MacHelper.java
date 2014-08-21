package com.yeepay.channel.cup.hn.pos.iso8583;

import java.util.Arrays;

import me.andpay.ti.base.AppBizException;
import me.andpay.ti.lnk.annotaion.Lnkwired;
import me.andpay.ti.s3.api.crypto.TxnAppCryptoService;
import me.andpay.ti.s3.api.kms.KeyDataWithCv;
import me.andpay.ti.util.ByteUtil;
import me.andpay.ti.util.HexUtil;

import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOPackager;

import com.yeepay.channel.cup.hn.encrypt.FilterData;
import com.yeepay.channel.cup.hn.encrypt.model.MacRequest;
import com.yeepay.channel.cup.hn.encrypt.model.MacRequest2;
import com.yeepay.channel.cup.hn.encrypt.model.MacResponse;
import com.yeepay.message.iso8583.Iso8583MacHelper;
import com.yeepay.message.iso8583.Iso8583Operator;
import com.yeepay.message.iso8583.Iso8583StandardFieldNoes;
import com.yeepay.socket.encrypt.ShortConSocket;

/**
 * ISO8583 MAC助手实现类 (银联MAC计算方式)
 * @author alex
 */
public class CupHnPosIso8583MacHelper implements Iso8583MacHelper {
	/**
	 * MAC域编号
	 */
	protected int macFieldNo = Iso8583StandardFieldNoes.FIELD_NO_MAC;

	/**
	 * 交易报文定义
	 */
	protected ISOPackager txnISOPackager;

	/**
	 * 交易应用加解密服务
	 */
	protected TxnAppCryptoService txnAppCryptoService;

	/**
	 * {@inheritDoc}
	 */
	public byte[] calcMac(ISOMsg isoMsg, String macKey) throws AppBizException {
		byte[] macStub = new byte[8];

		if (isoMsg.hasField(macFieldNo) == false) {
			// 没有MAC域时填充
			Iso8583Operator.setField(isoMsg, macFieldNo, macStub);
		}
		isoMsg.setPackager(txnISOPackager);

		byte[] mab = null;
		try {
			// 完整报文(含MAC)
			mab = isoMsg.pack();
		} catch (Throwable t) {
			throw new RuntimeException("Pack error when calc mac", t);
		}
		// MAC Block - 去除MAC域内容
		mab = ByteUtil.getBytes(mab, 0, mab.length - macStub.length);
		if (mab.length % 8 != 0) {
			// MAB长度补满8的倍数
			mab = ByteUtil.rightPad(mab, mab.length + 8 - mab.length % 8, (byte) 0x00);
		}

		byte[] calcBytes =  calcMacByEncription(mab, macKey);
		String calcStr = new String(calcBytes);
		
		calcBytes = HexUtil.decodeHex(calcStr);
		
		System.out.println("=====CalcStr:" + calcStr);
		return calcBytes;
//		return new byte[]{0x0A,0x0B,0x0C,0x0D,0x0E,0x0F,0x20,0x30};
		
		/*// TEK
		KeyDataWithCv tekByLmk = KeyDataWithCv.fromHex(macKey);
		
		//去除MAC域内容后，取前8字节进行DES
		byte[] desMacUnit = txnAppCryptoService.encryptDataByTek(tekByLmk, ByteUtil.getBytes(mab, 0, 8), false);
		
		byte[] mabUnit = desMacUnit;
		
		for (int offset = 8; offset < mab.length; offset += 8) {
			//计算结果后8字节进行XOR
			mabUnit = ByteUtil.xor(mabUnit, ByteUtil.getBytes(mab, offset, 8));
			// 用MAK对XOR结果进行DES
			mabUnit = txnAppCryptoService.encryptDataByTek(tekByLmk, mabUnit, false);
		}
		
		return mabUnit;*/
	}

	public byte[] calcMacByEncription(byte[] data, String mackey){
		byte[] result = null;
//		MacRequest macRequest = new MacRequest();
//		macRequest.setCmdCode("M0");
//		macRequest.setMacAlgorithm("2");
//		macRequest.setSekIndex("S1000");
//		macRequest.setMakPre("X");
//		macRequest.setMak(mackey.getBytes());
//		macRequest.setData(data);
		
		MacRequest2 macRequest = new MacRequest2();
		macRequest.setCmdCode("80");
		macRequest.setSekIndex("153");
		macRequest.setMacKey(mackey.getBytes());
		macRequest.setData(data);
		
		byte[] msgHead = new byte[0];
		FilterData requestFilter = new FilterData();
		requestFilter.setMsgCxt(macRequest.allData());
		requestFilter.setMsgHead(msgHead);
		byte[] requestByte = requestFilter.parseRequest();
		
		System.out.println("Send:" + HexUtil.encodeHex(requestByte));
		byte[] responseByte = ShortConSocket.sendMsg(requestByte);
		System.out.println("Rec:" + HexUtil.encodeHex(responseByte));
		
		if(responseByte != null && responseByte.length > 0){
			FilterData responseFilter = new FilterData();
			responseFilter.setMsgHead(msgHead);
			responseFilter.parseResponse(responseByte);
			if(responseFilter.getMsgCxt() != null){
				MacResponse macResponse =new MacResponse();
				macResponse.parseBytes(responseFilter.getMsgCxt());
				result = macResponse.getMac();
			}
		}
		return result;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void verifyMac(ISOMsg isoMsg, String macKey) throws AppBizException {
		byte[] actMac = Iso8583Operator.getFieldBinary(isoMsg, macFieldNo);
		byte[] expMac = calcMac(isoMsg, macKey);
		if (Arrays.equals(expMac, actMac) == false) {
			// MAC校验错误
			throw new RuntimeException(String.format("Mac verify error, expMac=%s, actMac=%s",
					HexUtil.encodeHex(expMac), HexUtil.encodeHex(actMac)));
		}
	}

	/**
	 * @param macFieldNo
	 *            the macFieldNo to set
	 */
	public void setMacFieldNo(int macFieldNo) {
		this.macFieldNo = macFieldNo;
	}

	/**
	 * @param txnISOPackager
	 *            the txnISOPackager to set
	 */
	public void setTxnISOPackager(ISOPackager txnISOPackager) {
		this.txnISOPackager = txnISOPackager;
	}

	/**
	 * @param txnAppCryptoService
	 *            the txnAppCryptoService to set
	 */
	@Lnkwired
	public void setTxnAppCryptoService(TxnAppCryptoService txnAppCryptoService) {
		this.txnAppCryptoService = txnAppCryptoService;
	}
}
