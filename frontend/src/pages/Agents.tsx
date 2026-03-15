import React, { useState } from 'react';
import { Heading, Table, Button, IconButton, Add16, Delete16, Edit16 } from '@jetbrains/ring-ui';

const Agents: React.FC = () => {
  const [agents] = useState<any[]>([]);

  return (
    <div className="agents-page">
      <div className="page-header">
        <Heading level={1}>Agent 管理</Heading>
        <Button icon={Add16} primary>添加 Agent</Button>
      </div>
      <Table>
        <Table.Header>
          <Table.Cell>名称</Table.Cell>
          <Table.Cell>模型</Table.Cell>
          <Table.Cell>状态</Table.Cell>
          <Table.Cell>操作</Table.Cell>
        </Table.Header>
        <Table.Body>
          {agents.length === 0 ? (
            <Table.Row>
              <Table.Cell colSpan={4} style={{ textAlign: 'center' }}>
                暂无 Agent 配置
              </Table.Cell>
            </Table.Row>
          ) : (
            agents.map((agent, index) => (
              <Table.Row key={index}>
                <Table.Cell>{agent.name}</Table.Cell>
                <Table.Cell>{agent.model}</Table.Cell>
                <Table.Cell>{agent.enabled ? '启用' : '禁用'}</Table.Cell>
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

export default Agents;
