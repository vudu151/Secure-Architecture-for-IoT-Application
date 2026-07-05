import React, { useEffect, useState } from 'react';
import { Card, Table, Tag, DatePicker, Select, Space, Row, Col, Typography, message } from 'antd';
import dayjs from 'dayjs';
import { adminApi } from '../api/adminApi';
import { formatDateTime } from '../constants';

const { RangePicker } = DatePicker;
const { Option } = Select;
const { Paragraph } = Typography;

const AuditLogsPage = () => {
  const [loading, setLoading] = useState(true);
  const [logs, setLogs] = useState([]);
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0,
  });
  const [filters, setFilters] = useState({
    action: undefined,
    dateRange: [dayjs().subtract(7, 'day'), dayjs()],
  });

  const loadLogs = async (page = 1, pageSize = 10, activeFilters = filters) => {
    setLoading(true);
    const params = {
      page: page - 1, // backend is 0-indexed
      size: pageSize,
    };

    if (activeFilters.action) {
      params.action = activeFilters.action;
    }
    if (activeFilters.dateRange && activeFilters.dateRange[0] && activeFilters.dateRange[1]) {
      params.from = activeFilters.dateRange[0].startOf('day').toISOString();
      params.to = activeFilters.dateRange[1].endOf('day').toISOString();
    }

    try {
      const { data } = await adminApi.getAuditLogs(params);
      const responseData = data.data || data;

      setLogs(responseData.content || []);
      setPagination({
        current: page,
        pageSize: pageSize,
        total: responseData.totalElements || 0,
      });
      setLoading(false);
    } catch (error) {
      setLoading(false);
      message.error('Không thể tải nhật ký bảo mật');
    }
  };

  useEffect(() => {
    loadLogs(1, pagination.pageSize);
  }, []);

  const handleTableChange = (newPagination) => {
    loadLogs(newPagination.current, newPagination.pageSize);
  };

  const handleActionFilter = (value) => {
    const updatedFilters = { ...filters, action: value };
    setFilters(updatedFilters);
    loadLogs(1, pagination.pageSize, updatedFilters);
  };

  const handleDateRangeChange = (val) => {
    if (val && val[0] && val[1]) {
      const updatedFilters = { ...filters, dateRange: val };
      setFilters(updatedFilters);
      loadLogs(1, pagination.pageSize, updatedFilters);
    }
  };

  const getActionColor = (action) => {
    if (action.includes('SUCCESS') || action.includes('CREATE') || action.includes('CHECK_IN')) return 'green';
    if (action.includes('FAILED') || action.includes('CANCEL') || action.includes('ERROR')) return 'red';
    if (action.includes('CONTROL') || action.includes('TOGGLE')) return 'orange';
    return 'blue';
  };

  const columns = [
    {
      title: 'Thời gian',
      dataIndex: 'createdAt',
      key: 'createdAt',
      render: (text) => formatDateTime(text),
      width: '180px',
    },
    {
      title: 'Hành động',
      dataIndex: 'action',
      key: 'action',
      render: (action) => (
        <Tag color={getActionColor(action)} style={{ fontWeight: 'bold' }}>
          {action}
        </Tag>
      ),
      width: '180px',
    },
    {
      title: 'Tài nguyên',
      dataIndex: 'resource',
      key: 'resource',
      render: (text) => <Tag color="geekblue">{text}</Tag>,
      width: '120px',
    },
    {
      title: 'Địa chỉ IP',
      dataIndex: 'ipAddress',
      key: 'ipAddress',
      width: '120px',
    },
    {
      title: 'Chi tiết hoạt động',
      dataIndex: 'details',
      key: 'details',
      ellipsis: true,
    },
  ];

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      {/* Filter panel */}
      <Card className="glass-card" bordered={false}>
        <Row gutter={[16, 16]} align="middle">
          <Col xs={24} sm={8}>
            <Space direction="vertical" style={{ width: '100%' }}>
              <span style={{ fontWeight: 'bold' }}>Lọc theo hành động:</span>
              <Select
                placeholder="Chọn loại hành động"
                style={{ width: '100%' }}
                value={filters.action}
                onChange={handleActionFilter}
                allowClear
              >
                <Option value="USER_LOGIN">USER_LOGIN</Option>
                <Option value="USER_LOGIN_FAILED">USER_LOGIN_FAILED</Option>
                <Option value="USER_REGISTER">USER_REGISTER</Option>
                <Option value="USER_LOGOUT">USER_LOGOUT</Option>
                <Option value="BOOKING_CREATED">BOOKING_CREATED</Option>
                <Option value="BOOKING_CANCELLED">BOOKING_CANCELLED</Option>
                <Option value="BOOKING_EXPIRED">BOOKING_EXPIRED</Option>
                <Option value="VEHICLE_CHECK_IN">VEHICLE_CHECK_IN</Option>
                <Option value="VEHICLE_CHECK_OUT">VEHICLE_CHECK_OUT</Option>
                <Option value="GATE_CONTROL">GATE_CONTROL</Option>
                <Option value="USER_STATUS_TOGGLE">USER_STATUS_TOGGLE</Option>
              </Select>
            </Space>
          </Col>
          <Col xs={24} sm={12}>
            <Space direction="vertical" style={{ width: '100%' }}>
              <span style={{ fontWeight: 'bold' }}>Khoảng thời gian:</span>
              <RangePicker
                value={filters.dateRange}
                onChange={handleDateRangeChange}
                allowClear={false}
                style={{ width: '100%' }}
              />
            </Space>
          </Col>
        </Row>
      </Card>

      {/* Logs Table */}
      <Card className="glass-card" title="Nhật ký kiểm toán bảo mật (mTLS & API logging)" bordered={false}>
        <Table
          dataSource={logs}
          columns={columns}
          rowKey="id"
          pagination={pagination}
          loading={loading}
          onChange={handleTableChange}
          expandable={{
            expandedRowRender: (record) => (
              <div style={{ padding: '8px 24px', background: '#fafafa', borderRadius: 8 }}>
                <Paragraph style={{ margin: 0 }}>
                  <strong>Nội dung chi tiết (JSON Payload):</strong>
                  <pre
                    style={{
                      marginTop: 8,
                      padding: 12,
                      background: '#f0f2f5',
                      borderRadius: 4,
                      overflowX: 'auto',
                      fontSize: 12,
                    }}
                  >
                    {JSON.stringify(record, null, 2)}
                  </pre>
                </Paragraph>
              </div>
            ),
          }}
          size="middle"
        />
      </Card>
    </Space>
  );
};

export default AuditLogsPage;
