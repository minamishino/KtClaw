import React, { useState } from 'react';
import { Heading, Table, Button, IconButton, Add16, Delete16, Edit16 } from '@jetbrains/ring-ui';

const Models: React.FC = () => {
  const [models] = useState<any[]>([]);

  return (
    <div className="models-page">
      <div className="page-header">
        <Heading level={1}>模型配置</Heading>
        <Button icon={Add16} primary>添加模型</Button>
      </div>
      <Table>
        <Table.Header>
          <Table.Cell>模型名称</Table.Cell>
          <Table.Cell>提供者</Table.Cell>
          <Table.Cell>API 地址</Table.Cell>
          <Table.Cell>操作</Table.Cell>
        </Table.Header>
        <Table.Body>
          {models.length === 0 ? (
            <Table.Row>
              <Table.Cell colSpan={4} style={{ textAlign: 'center' }}>
                暂无模型配置
              </Table.Cell>
            </Table.Row>
          ) : (
            models.map((model, index) => (
              <Table.Row key={index}>
                <Table.Cell>{model.name}</Table.Cell>
                <Table.Cell>{model.provider}</Table.Cell>
                <Table.Cell>{model.apiUrl}</Table.Cell>
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

export default Models;
