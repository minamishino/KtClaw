import React from 'react';
import { Heading, Card, Button } from '@jetbrains/ring-ui';

const Logs: React.FC = () => {
  return (
    <div className="logs-page">
      <div className="page-header">
        <Heading level={1}>日志查看</Heading>
        <Button>刷新</Button>
      </div>
      <Card>
        <div className="logs-container">
          <pre className="logs-content">暂无日志</pre>
        </div>
      </Card>
    </div>
  );
};

export default Logs;
