package com.yeepay.channel.cup.hn.pos.iso8583.helper;

import static com.yeepay.message.TxnPropNames.ADITIONAL_DATA;
import me.andpay.ti.base.AppBizException;

import org.jpos.iso.ISOMsg;

import com.yeepay.message.TxnContext;
import com.yeepay.message.iso8583.Iso8583BitMap;
import com.yeepay.message.iso8583.Iso8583FieldTransferHelper;
import com.yeepay.message.iso8583.Iso8583Operator;
import com.yeepay.message.iso8583.Iso8583StandardFieldNoes;

/**
 * Description: 附加数据域转换辅助器
 * @author Kevin
 * @Createdate 2014年7月22日.
 **/

public class Field48DefaultTransferHelper implements Iso8583FieldTransferHelper {

	public int getFieldNo() {
		return Iso8583StandardFieldNoes.FIELD_NO_ATTACH_DATA;
	}

	/**
	 * {@inheritDoc}
	 */
	public void checkFieldInfo(ISOMsg isoMsg, TxnContext txnCtx, Iso8583BitMap iso8583BitMap) throws AppBizException {
		throw new RuntimeException("Unexpected check, fieldNo=" + getFieldNo());
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean getFieldInfo(ISOMsg isoMsg, TxnContext txnCtx, Iso8583BitMap iso8583BitMap) throws AppBizException {
		String aditionalData = Iso8583Operator.getFieldString(isoMsg, getFieldNo());
		txnCtx.setProperty(ADITIONAL_DATA, aditionalData);
		return (aditionalData != null);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean setFieldInfo(ISOMsg isoMsg, TxnContext txnCtx, Iso8583BitMap iso8583BitMap) throws AppBizException {
		return Iso8583Operator.setField(isoMsg, getFieldNo(), txnCtx.getStringProperty(ADITIONAL_DATA));
	}

}
