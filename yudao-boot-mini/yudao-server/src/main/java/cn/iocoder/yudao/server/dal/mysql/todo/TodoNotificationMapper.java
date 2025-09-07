package cn.iocoder.yudao.server.dal.mysql.todo;

import cn.iocoder.yudao.server.dal.dataobject.todo.TodoNotificationDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 待办通知 Mapper
 * 
 * @author 芋道源码
 */
@Mapper
public interface TodoNotificationMapper extends BaseMapper<TodoNotificationDO> {

    // 基础CRUD方法由 BaseMapper 提供
    // 如需复杂查询，可在此添加自定义方法

}