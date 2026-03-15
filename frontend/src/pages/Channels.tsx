import React, { useState } from 'react';
import { Heading, Table, Button, IconButton, Add16, Delete16, Edit16 } from '@jetbrains/ring-ui';

const Channels: React.FC = () => {
  const [channels] = useState<any[]>([]);

  return (
    <div className="channels-page">
      <div className="page-header">
        <Heading level={1}>频道配置</Heading>
        <Button icon={Add16} primary>添加频道</Button>
      </div>
      <Table>
        <Table.Header>
          <Table.Cell>频道名称</Table.Cell>
          <Table.Cell>类型</Table.Cell>
          <Table.Cell>状态</Table.Cell>
          <Table.Cell>操作</Table.Cell>
        </Table.Header>
        <Table.Body>
          {channels.length === 0 ? (
            <Table.Row>
              <Table.Cell colSpan={4} style={{ textAlign: 'center' }}>
                暂无频道配置
              </Table.Cell>
            </Table.Row>
          ) : (
            channels.map((channel, index) => (
              <Table.Row key={index}>
                <Table.Cell>{channel.name}</Table.Cell>
                <Table.Cell>{channel.type}</Table.Cell>
                <Table.Cell>{channel.enabled ? '启用' : '禁用'}</Table.Cell>
                <Table.Cell>
                  <IconButton icon={Edit16} title="编辑" />
                  <IconButton icon={Delete16} title="删除" />
                </Table.Cell>
              </Table.Row>
            ))
          )}
        </Table.Body>
      </Table>
    </div>
  );
};

export default Channels;
