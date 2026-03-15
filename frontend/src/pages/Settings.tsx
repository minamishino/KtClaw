import React from 'react';
import { Heading, Form, TextInput, Button, Card } from '@jetbrains/ring-ui';

const Settings: React.FC = () => {
  return (
    <div className="settings-page">
      <Heading level={1}>系统设置</Heading>
      <Card className="settings-card">
        <Form>
          <Form.Field label="服务器地址" name="serverUrl">
            <TextInput name="serverUrl" placeholder="http://localhost:8080" />
          </Form.Field>
          <Form.Field label="日志级别" name="logLevel">
            <TextInput name="logLevel" placeholder="info" />
          </Form.Field>
          <Form.Actions>
            <Button primary>保存设置</Button>
          </Form.Actions>
        </Form>
      </Card>
    </div>
  );
};

export default Settings;
