import React from 'react';
import { BrowserRouter as Router, Routes, Route, Link, useLocation } from 'react-router-dom';
import { Sidebar, Layout, Heading } from '@jetbrains/ring-ui';
import {
  Dashboard16,
  Agent16,
  Channel16,
  Model16,
  Log16,
  Settings16
} from '@jetbrains/icons';

import Dashboard from './pages/Dashboard';
import Agents from './pages/Agents';
import Channels from './pages/Channels';
import Models from './pages/Models';
import Logs from './pages/Logs';
import Settings from './pages/Settings';

import './App.css';

const SidebarItem: React.FC<{
  icon: React.ReactNode;
  title: string;
  to: string;
  active: boolean;
}> = ({ icon, title, to, active }) => {
  return (
    <Link to={to} className={`sidebar-item ${active ? 'active' : ''}`}>
      <span className="sidebar-icon">{icon}</span>
      <span className="sidebar-title">{title}</span>
    </Link>
  );
};

const AppContent: React.FC = () => {
  const location = useLocation();

  const menuItems = [
    { path: '/', icon: <Dashboard16 />, title: '仪表盘', component: Dashboard },
    { path: '/agents', icon: <Agent16 />, title: 'Agent 管理', component: Agents },
    { path: '/channels', icon: <Channel16 />, title: '频道配置', component: Channels },
    { path: '/models', icon: <Model16 />, title: '模型配置', component: Models },
    { path: '/logs', icon: <Log16 />, title: '日志查看', component: Logs },
    { path: '/settings', icon: <Settings16 />, title: '系统设置', component: Settings },
  ];

  return (
    <Layout>
      <Sidebar className="sidebar">
        <div className="sidebar-header">
          <Heading level={2}>KtClaw</Heading>
        </div>
        <div className="sidebar-menu">
          {menuItems.map((item) => (
            <SidebarItem
              key={item.path}
              icon={item.icon}
              title={item.title}
              to={item.path}
              active={location.pathname === item.path}
            />
          ))}
        </div>
      </Sidebar>
      <div className="main-content">
        <Routes>
          {menuItems.map((item) => (
            <Route key={item.path} path={item.path} element={<item.component />} />
          ))}
        </Routes>
      </div>
    </Layout>
  );
};

const App: React.FC = () => {
  return (
    <Router>
      <AppContent />
    </Router>
  );
};

export default App;

