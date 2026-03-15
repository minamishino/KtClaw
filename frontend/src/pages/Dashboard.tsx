import React from 'react';
import { Grid, Row, Col, Card, Heading, Text } from '@jetbrains/ring-ui';
import { Info16 } from '@jetbrains/icons';

import './Dashboard.css';

const Dashboard: React.FC = () => {
  return (
    <div className="dashboard">
      <Heading level={1}>仪表盘</Heading>
      <Grid>
        <Row>
          <Col xs={12} sm={6} md={4}>
            <Card>
              <div className="stat-card">
                <Info16 className="stat-icon" />
                <Heading level={3}>总 Agents</Heading>
                <Text large className="stat-value">0</Text>
              </div>
            </Card>
          </Col>
          <Col xs={12} sm={6} md={4}>
            <Card>
              <div className="stat-card">
                <Info16 className="stat-icon" />
                <Heading level={3}>活跃频道</Heading>
                <Text large className="stat-value">0</Text>
              </div>
            </Card>
          </Col>
          <Col xs={12} sm={6} md={4}>
            <Card>
              <div className="stat-card">
                <Info16 className="stat-icon" />
                <Heading level={3}>已加载模型</Heading>
                <Text large className="stat-value">0</Text>
              </div>
            </Card>
          </Col>
        </Row>
      </Grid>
    </div>
  );
};

export default Dashboard;
