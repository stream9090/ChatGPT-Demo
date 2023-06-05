package com.stream;

import javax.annotation.Resource;

import com.alibaba.druid.util.StringUtils;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class BizScanCustFacadeImpl implements BizScanCustFacade {

	@Autowired
	private CommonBiz commonBiz;

	@Resource
	private LockRedisUtils lockRedisUtils;

	@Resource
	private SqrcTemplateManager sqrcTemplateManager;

	@Resource
	private IProductIdWhitelistService productIdWhitelistService;

	@SuppressWarnings("unchecked")
	@Override
	public BizScanCustResult bizScanCust(BizScanCustDTO bizScanCustDTO) {
		log.info("扫码收银台-收到反扫交易请求{}", JSONObject.toJSONString(bizScanCustDTO));
		BizScanCustResult result = new BizScanCustResult();
		try {
			//白名单校验
			if(!productIdWhitelistService.inWhiteList(bizScanCustDTO.getProductId())){
				throw new BizException(ReturnCodeEnum.METHOD_INVOKE_LIMITED);
			}
			SqrcTemplateEnum sqrcTemplate = null;
			if(!StringUtils.isEmpty(bizScanCustDTO.getPayTransType()) &&
					TransTypeEnum.AT_B_SCAN_C_PAY.getTransTypeCode().equals(bizScanCustDTO.getPayTransType())){
				// 分布式上锁
				lockRedisUtils.lockRequest(bizScanCustDTO.getProductId(),bizScanCustDTO.getProductSeqId(),
						bizScanCustDTO.getProductSeqDate(),bizScanCustDTO.getProductSeqTime(), SqrcBizFacadeEnum.AT_SCANCUST_PAY_SYNC.getTransType());
				sqrcTemplate = SqrcTemplateEnum.valueOf(SqrcBizFacadeEnum.AT_SCANCUST_PAY_SYNC.getTemplateName());
			} else if(!StringUtils.isEmpty(bizScanCustDTO.getPayTransType()) &&
					TransTypeEnum.HLB_SCAN_PAY.getTransTypeCode().equals(bizScanCustDTO.getPayTransType())){
				// 分布式上锁
                lockRedisUtils.lockRequest(bizScanCustDTO.getProductId(),bizScanCustDTO.getProductSeqId(),
                        bizScanCustDTO.getProductSeqDate(),bizScanCustDTO.getProductSeqTime(), SqrcBizFacadeEnum.HLB_SCAN_PAY.getTransType());
                sqrcTemplate = SqrcTemplateEnum.valueOf(SqrcBizFacadeEnum.HLB_SCAN_PAY.getTemplateName());
			}  if(!StringUtils.isEmpty(bizScanCustDTO.getPayTransType()) &&
					TransTypeEnum.HLB_AGGREGATION_SCAN_PAY.getTransTypeCode().equals(bizScanCustDTO.getPayTransType())){
				// 分布式上锁
                lockRedisUtils.lockRequest(bizScanCustDTO.getProductId(),bizScanCustDTO.getProductSeqId(),
                        bizScanCustDTO.getProductSeqDate(),bizScanCustDTO.getProductSeqTime(), SqrcBizFacadeEnum.HLB_AGGREGATION_SCAN_PAY.getTransType());
                sqrcTemplate = SqrcTemplateEnum.valueOf(SqrcBizFacadeEnum.HLB_AGGREGATION_SCAN_PAY.getTemplateName());
			} else{
				// 分布式上锁
				lockRedisUtils.lockRequest(bizScanCustDTO.getProductId(),bizScanCustDTO.getProductSeqId(),
						bizScanCustDTO.getProductSeqDate(),bizScanCustDTO.getProductSeqTime(), SqrcBizFacadeEnum.SCANCUST_PAY_SYNC.getTransType());
				sqrcTemplate = SqrcTemplateEnum.valueOf(SqrcBizFacadeEnum.SCANCUST_PAY_SYNC.getTemplateName());
			}
			// 根据模板懒加载processService
			sqrcTemplateManager.createSqrcTemplateContext(sqrcTemplate);
			/* get Service from template */
			SqrcProcessService<CommonPayBO, WSResultBO> service = sqrcTemplate.getSqrcProcessService();
			BizScanCustBO bizScanCustBO = buildScanCustBO(bizScanCustDTO);
			ScanCustPayResultBO scanCustPayResultBO = new ScanCustPayResultBO();
			service.doExecute(bizScanCustBO,scanCustPayResultBO, sqrcTemplate.getSqrcTemplate());
			result = SqrcTransConverter.converterScanCustPayRequestDTO(scanCustPayResultBO);
		} catch (BizException ex) {
			log.error("扫码收银台-收银台反扫交易应答异常BizException:{}",ex);
			result.setReturnCode(ex.getCode());
			result.setReturnDesc(ex.getMessage());
		} catch (Exception ex) {
			log.error("扫码收银台-收银台反扫交易应答异常Exception:{}",ex);
			result.setReturnCode(ReturnCodeEnum.SYSTEM_ERROR.getReturnCode());
			result.setReturnDesc(ReturnCodeEnum.SYSTEM_ERROR.getCodeDesc());
		}
		log.info("扫码收银台-收银台反扫交易应答为  ：{}", ValueFilterUtils.filterSensitive(result));
		return result;
	}

	private BizScanCustBO buildScanCustBO(BizScanCustDTO bizScanCustDTO) {
		BizScanCustBO bizScanCustBO = new BizScanCustBO();
		commonBiz.buildCommonBO(bizScanCustDTO, bizScanCustBO);
		bizScanCustBO.setHbFqNum(bizScanCustDTO.getHbFqNum());
		bizScanCustBO.setOrderId(bizScanCustDTO.getOrderId());
		bizScanCustBO.setAttachInfo(bizScanCustDTO.getAttachInfo());
		bizScanCustBO.setAuthCode(bizScanCustDTO.getAuthCode());
		bizScanCustBO.setDeviceInfo(bizScanCustDTO.getDeviceInfo());
		bizScanCustBO.setOpUserId(bizScanCustDTO.getOpUserId());
		bizScanCustBO.setTradeRoute(bizScanCustDTO.getTradeRoute());
		bizScanCustBO.setAttachInfo(bizScanCustDTO.getAttachInfo());
		bizScanCustBO.setTransAmt(bizScanCustDTO.getTransAmt());
		bizScanCustBO.setTransNotifyUrl(bizScanCustDTO.getTransNotifyUrl());
		bizScanCustBO.setPayChannelType(bizScanCustDTO.getPayChannelType());
		bizScanCustBO.setGoodsDesc(bizScanCustDTO.getGoodsDesc());
		bizScanCustBO.setLimitPay(bizScanCustDTO.getLimitPay());
		bizScanCustBO.setGoodsType(bizScanCustDTO.getGoodsType());
		bizScanCustBO.setTradeValidateCode(bizScanCustDTO.getTradeValidateCode());
		bizScanCustBO.setAddtionalFeeMap(bizScanCustDTO.getAddtionalFeeMap());
		bizScanCustBO.setAppId(bizScanCustDTO.getAppId());
		bizScanCustBO.setGoodsId(bizScanCustDTO.getGoodsId());
		bizScanCustBO.setGoodsName(bizScanCustDTO.getGoodsName());
		bizScanCustBO.setWxpayGoodsId(bizScanCustDTO.getWxpayGoodsId());
		bizScanCustBO.setQuantity(bizScanCustDTO.getQuantity());
		bizScanCustBO.setPrice(bizScanCustDTO.getPrice());
		bizScanCustBO.setHbFqNum(bizScanCustDTO.getHbFqNum());
		bizScanCustBO.setHbFqSellerPercent(bizScanCustDTO.getHbFqSellerPercent());
		bizScanCustBO.setHbFqMerDiscountFlag(bizScanCustDTO.getHbFqMerDiscountFlag());
		bizScanCustBO.setGoodTag(bizScanCustDTO.getGoodTag());
		bizScanCustBO.setFqChannels(bizScanCustDTO.getFqChannels());
		bizScanCustBO.setPayTransType(bizScanCustDTO.getPayTransType());
		bizScanCustBO.setAppAuthToken(bizScanCustDTO.getAppAuthToken());
		bizScanCustBO.setChannelId(bizScanCustDTO.getChannelId());
		bizScanCustBO.setOutTradeNo(bizScanCustDTO.getOutTradeNo());
		bizScanCustBO.setSubAppId(bizScanCustDTO.getSubAppId());
		bizScanCustBO.setTradeScene(bizScanCustDTO.getTradeScene());
		bizScanCustBO.setSubOpenId(bizScanCustDTO.getSubOpenId());
		commonBiz.buildDetailBO(bizScanCustBO,bizScanCustDTO.getDivInfoList());
		if(Constants.FEE_FLAG.equals(bizScanCustBO.getFeeFlag())){
			commonBiz.buildFeeDetails(bizScanCustBO,bizScanCustDTO.getFeeInfoList());
		}
		return bizScanCustBO;
	}

}
