import React, { useState } from 'react';
import { Layout, Menu, Button, Avatar, Dropdown, Space, Typography } from 'antd';
import {
  MenuUnfoldOutlined,
  MenuFoldOutlined,
  DashboardOutlined,
  CarOutlined,
  DollarOutlined,
  TeamOutlined,
  ApiOutlined,
  FileProtectOutlined,
  LogoutOutlined,
  UserOutlined,
} from '@ant-design/icons';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import useAuthStore from '../stores/authStore';
import logo from '../assets/react.svg'; // fallback standard asset

const { Header, Sider, Content } = Layout;
const { Text } = Typography;

const AdminLayout = () => {
  const [collapsed, setCollapsed] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const { user, logout } = useAuthStore();

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  const menuItems = [
    {
      key: '/dashboard',
      icon: <DashboardOutlined />,
      label: 'Bảng điều khiển',
    },
    {
      key: '/parking-map',
      icon: <CarOutlined />,
      label: 'Bản đồ bãi đỗ',
    },
    {
      key: '/revenue',
      icon: <DollarOutlined />,
      label: 'Báo cáo doanh thu',
    },
    {
      key: '/users',
      icon: <TeamOutlined />,
      label: 'Quản lý tài xế',
    },
    {
      key: '/devices',
      icon: <ApiOutlined />,
      label: 'Giám sát thiết bị',
    },
    {
      key: '/audit-logs',
      icon: <FileProtectOutlined />,
      label: 'Nhật ký bảo mật',
    },
  ];

  const userDropdownItems = {
    items: [
      {
        key: 'email',
        label: <Text strong>{user?.email || 'Admin'}</Text>,
        disabled: true,
      },
      {
        type: 'divider',
      },
      {
        key: 'logout',
        icon: <LogoutOutlined />,
        label: 'Đăng xuất',
        onClick: handleLogout,
      },
    ],
  };

  const handleMenuClick = ({ key }) => {
    navigate(key);
  };

  // Find page title based on current path
  const currentMenuItem = menuItems.find((item) => location.pathname === item.key);
  const pageTitle = currentMenuItem ? currentMenuItem.label : 'Smart Parking Admin';

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider
        trigger={null}
        collapsible
        collapsed={collapsed}
        theme="dark"
        className="antd-sidebar"
        width={240}
        style={{
          overflow: 'auto',
          height: '100vh',
          position: 'fixed',
          left: 0,
          top: 0,
          bottom: 0,
        }}
      >
        {/* Sider Logo Section */}
        <div
          style={{
            height: 64,
            display: 'flex',
            alignItems: 'center',
            justifyContent: collapsed ? 'center' : 'flex-start',
            padding: collapsed ? '0' : '0 24px',
            background: '#002140',
            transition: 'all 0.2s',
          }}
        >
          <Space size="middle">
            <CarOutlined style={{ fontSize: 24, color: '#1677ff' }} />
            {!collapsed && (
              <span
                style={{
                  color: '#fff',
                  fontSize: 16,
                  fontWeight: 'bold',
                  letterSpacing: '1px',
                  whiteSpace: 'nowrap',
                }}
              >
                SPARK ADMIN
              </span>
            )}
          </Space>
        </div>
        
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[location.pathname]}
          items={menuItems}
          onClick={handleMenuClick}
          style={{ padding: '16px 0' }}
        />
      </Sider>
      
      <Layout style={{ marginLeft: collapsed ? 80 : 240, transition: 'all 0.2s' }}>
        <Header className="antd-header">
          <Button
            type="text"
            icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
            onClick={() => setCollapsed(!collapsed)}
            style={{ fontSize: '16px', width: 64, height: 64 }}
          />
          
          <span style={{ fontSize: 18, fontWeight: 'bold', color: '#1f1f1f', flex: 1, marginLeft: 16 }}>
            {pageTitle}
          </span>

          <Space size="large">
            <Dropdown menu={userDropdownItems} placement="bottomRight" trigger={['click']}>
              <Space style={{ cursor: 'pointer' }}>
                <Avatar style={{ backgroundColor: '#1a237e' }} icon={<UserOutlined />} />
                {!collapsed && <span style={{ color: '#595959' }}>{user?.fullName || 'Quản trị viên'}</span>}
              </Space>
            </Dropdown>
          </Space>
        </Header>
        
        <Content
          style={{
            margin: '24px 24px 0',
            padding: 24,
            minHeight: 280,
            overflow: 'initial',
          }}
        >
          <Outlet />
        </Content>
        
        <div style={{ textAlign: 'center', padding: '24px 0', color: '#8c8c8c' }}>
          Smart Parking System ©2026. Secure IoT Architecture Project (HUST).
        </div>
      </Layout>
    </Layout>
  );
};

export default AdminLayout;
