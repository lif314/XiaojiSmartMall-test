package com.lif314.gulimall.product.service.impl;

import com.lif314.common.constant.ProductConstant;
import com.lif314.gulimall.product.dao.AttrAttrgroupRelationDao;
import com.lif314.gulimall.product.dao.AttrGroupDao;
import com.lif314.gulimall.product.dao.CategoryDao;
import com.lif314.gulimall.product.entity.AttrAttrgroupRelationEntity;
import com.lif314.gulimall.product.entity.AttrGroupEntity;
import com.lif314.gulimall.product.entity.CategoryEntity;
import com.lif314.gulimall.product.service.CategoryService;
import com.lif314.gulimall.product.vo.AttrGroupRelationVo;
import com.lif314.gulimall.product.vo.AttrRespVo;
import com.lif314.gulimall.product.vo.AttrVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lif314.common.utils.PageUtils;
import com.lif314.common.utils.Query;

import com.lif314.gulimall.product.dao.AttrDao;
import com.lif314.gulimall.product.entity.AttrEntity;
import com.lif314.gulimall.product.service.AttrService;
import org.springframework.transaction.annotation.Transactional;


@Service("AttrService")
public class AttrServiceImpl extends ServiceImpl<AttrDao, AttrEntity> implements AttrService {


    @Autowired
    AttrAttrgroupRelationDao attrAttrgroupRelationDao;

    @Autowired
    AttrGroupDao attrGroupDao;

    @Autowired
    CategoryDao categoryDao;

    @Autowired
    CategoryService categoryService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrEntity> page = this.page(
                new Query<AttrEntity>().getPage(params),
                new QueryWrapper<AttrEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * ??????vo??????
     * <p>
     * ?????????????????????????????????????????????
     */
    @Override
    public void saveAttr(AttrVo attrVo) {
        // 1 ??????????????????
        AttrEntity attrEntity = new AttrEntity();  // PO
        // ?????????????????????????????? source target
        BeanUtils.copyProperties(attrVo, attrEntity);
        this.save(attrEntity);

        // 2. ??????????????????
        if (attrVo.getAttrType() == ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode() && attrVo.getAttrGroupId() != null) { // ????????????
            AttrAttrgroupRelationEntity attrAttrgroupRelationEntity = new AttrAttrgroupRelationEntity();
            attrAttrgroupRelationEntity.setAttrId(attrEntity.getAttrId());
            attrAttrgroupRelationEntity.setAttrGroupId(attrVo.getAttrGroupId());
            attrAttrgroupRelationDao.insert(attrAttrgroupRelationEntity);
        }

    }

    /**
     * ????????????????????????
     */
    @Override
    public PageUtils queryBaseAttrPage(Map<String, Object> params, Long catelogId, String attrType) {
        // ???????????? ??????????????????
        QueryWrapper<AttrEntity> queryWrapper = new QueryWrapper<AttrEntity>().eq(
                "attr_type", "base".equalsIgnoreCase(attrType) ?
                        ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode() : ProductConstant.AttrEnum.ATTR_TYPE_SALE.getCode());
        ;

        // ??????????????? id = 0
        if (catelogId != 0) {
            // ??????????????????
            queryWrapper.eq("catelog_id", catelogId);
        }

        // ????????????
        String key = (String) params.get("key");
        if (!StringUtils.isEmpty(key)) {
            queryWrapper.and((wrapper) -> {
                wrapper.eq("attr_name", key).or().like("attr_id", key);
            });
        }
        IPage<AttrEntity> page = this.page(
                new Query<AttrEntity>().getPage(params),
                queryWrapper
        );

        /**
         * ???????????????
         * 			"catelogName": "??????/??????/??????", //??????????????????
         * 			"groupName": "??????", //??????????????????
         */
        PageUtils pageUtils = new PageUtils(page);
        // ??????????????????
        List<AttrEntity> records = page.getRecords();
        // ????????????
        List<AttrRespVo> respVos = records.stream().map((attrEntity) -> {
            AttrRespVo attrRespVo = new AttrRespVo();
            BeanUtils.copyProperties(attrEntity, attrRespVo);

            /**
             * ????????????????????????name
             */
            if ("base".equalsIgnoreCase(attrType)) { // ??????????????????????????????
                // ???????????????????????????id
                AttrAttrgroupRelationEntity attrAttrgroupRelationEntity = attrAttrgroupRelationDao.selectOne(
                        new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attrEntity.getAttrId()));

                if (attrAttrgroupRelationEntity != null && attrAttrgroupRelationEntity.getAttrGroupId() != null) {
                    Long attrGroupId = attrAttrgroupRelationEntity.getAttrGroupId();

                    // ????????????id??????name
                    AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(attrGroupId);
                    if(attrGroupEntity != null){
                        attrRespVo.setGroupName(attrGroupEntity.getAttrGroupName());
                    }
                }
            }

            // ??????name
            CategoryEntity categoryEntity = categoryDao.selectById(attrEntity.getCatelogId());
            if (categoryEntity != null) {
                attrRespVo.setCatelogName(categoryEntity.getName());
            }
            return attrRespVo;
        }).collect(Collectors.toList());

        pageUtils.setList(respVos);
        return pageUtils;
    }

    /**
     * ??????????????????
     */
    @Override
    public AttrRespVo getAttrInfo(Long attrId) {
        AttrRespVo respVo = new AttrRespVo();
        // ??????????????????
        AttrEntity attrEntity = this.getById(attrId);
        BeanUtils.copyProperties(attrEntity, respVo);

        // ??????????????????????????????
        if (attrEntity.getAttrType() == ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode()) {
            // ???????????????????????????id
            AttrAttrgroupRelationEntity relationEntity = attrAttrgroupRelationDao.selectOne(
                    new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attrId));
            if (relationEntity != null) {
                respVo.setAttrGroupId(relationEntity.getAttrGroupId());
            }
        }

        // ????????????
        Long[] catelogPath = categoryService.findCatelogPath(attrEntity.getCatelogId());
        if (catelogPath != null) {
            respVo.setCatelogPath(catelogPath);
        }
        return respVo;
    }

    @Transactional
    @Override
    public void updateAttr(AttrVo attrVo) {
        AttrEntity attrEntity = new AttrEntity();
        BeanUtils.copyProperties(attrVo, attrEntity);
        // ??????????????????
        this.updateById(attrEntity);

        /**
         * ???????????????????????????????????????????????????????????? ??????????????????
         */

        if (attrEntity.getAttrType() == ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode()) {
            Long selectCount = attrAttrgroupRelationDao.selectCount(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attrVo.getAttrId()));
            // ??????????????????
            AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();
            relationEntity.setAttrGroupId(attrVo.getAttrGroupId());
            relationEntity.setAttrId(attrVo.getAttrId());
            if (selectCount > 0) {
                // ??????
                attrAttrgroupRelationDao.update(relationEntity, new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attrVo.getAttrId()));
            } else {
                attrAttrgroupRelationDao.insert(relationEntity);
            }
        }
    }

    /**
     * ???????????????????????????????????????
     */
    @Override
    public List<AttrEntity> getAttrRelation(Long attrgroupId) {
        // ?????????????????????????????????attr_id,??????attr????????????
        List<AttrAttrgroupRelationEntity> entities = attrAttrgroupRelationDao.selectList(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_group_id", attrgroupId));

        // ??????id
        List<Long> attrIds = entities.stream().map((attr) -> {
            return attr.getAttrId();
        }).collect(Collectors.toList());

        List<AttrEntity> attrEntities = new ArrayList<>();
        if (attrIds != null && attrIds.size() > 0) {
            attrEntities = this.listByIds(attrIds);
        }
        return attrEntities;
    }

    /**
     * ??????????????????
     */
    @Override
    public void deleteRelation(AttrGroupRelationVo[] relationVos) {
//        attrAttrgroupRelationDao.delete(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", 1L).eq("attr_group_id", 1l));
        // ????????????????????????????????????????????????

        // ?????????vo??????????????????????????????Dao?????????????????????????????????vo???????????????????????????
        List<AttrAttrgroupRelationEntity> relationEntities = Arrays.asList(relationVos).stream().map((item) -> {
            AttrAttrgroupRelationEntity entity = new AttrAttrgroupRelationEntity();
            BeanUtils.copyProperties(item, entity);
            return entity;
        }).collect(Collectors.toList());


        attrAttrgroupRelationDao.deleteBatchRelation(relationEntities);

    }

    /**
     * ?????????????????????????????????????????????????????????????????????????????????????????????????????????
     */
    @Override
    public PageUtils getAttrNoRelationAttr(Long attrgroupId, Map<String, Object> params) {
        // 1. ??????????????????????????????????????????????????????????????????
        // ????????????????????????id
        AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(attrgroupId);
        Long catelogId = attrGroupEntity.getCatelogId();

        // 2. ?????????????????????????????????????????????????????????
        // 2.1 ????????????????????????????????????????????????????????????
        List<AttrGroupEntity> groupEntities = attrGroupDao.selectList(
                new QueryWrapper<AttrGroupEntity>().eq("catelog_id", catelogId));
        // ??????id??????
        List<Long> groupIds = groupEntities.stream().map(AttrGroupEntity::getAttrGroupId).collect(Collectors.toList());
        // 2.2 ???????????????????????????

        // ?????????????????????
        List<AttrAttrgroupRelationEntity> attr_group = attrAttrgroupRelationDao.selectList(new QueryWrapper<AttrAttrgroupRelationEntity>().in("attr_group_id", groupIds));
        // ???????????????????????????
        List<Long> attrIds = attr_group.stream().map(AttrAttrgroupRelationEntity::getAttrId).collect(Collectors.toList());

        // 2.3 ??????????????????????????????????????????????????? -- ?????????????????????
        QueryWrapper<AttrEntity> wrapper =
                new QueryWrapper<AttrEntity>().eq("catelog_id", catelogId).eq("attr_type", ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode());
        if(attrIds!= null && attrIds.size() > 0){
            wrapper.notIn("attr_id", attrIds);
        }

        // ????????????
        String key = (String) params.get("key");
        if (!StringUtils.isEmpty(key)) {
            wrapper.and((w -> {
                 w.eq("attr_id", key).or().like("attr_name", key);
            }));
        }

        // ????????????
        IPage<AttrEntity> page = this.page(new Query<AttrEntity>().getPage(params), wrapper);

        return new PageUtils(page);
    }

}
