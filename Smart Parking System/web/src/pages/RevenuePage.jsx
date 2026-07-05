import React, { useEffect, useState } from 'react';
import { Card, DatePicker, Row, Col, Statistic, Table, Space, message, Spin } from 'antd';
import { DollarOutlined, CalendarOutlined, TransactionOutlined } from '@ant-design/icons';
import { Column } from '@ant-design/charts';
import dayjs from 'dayjs';
import { adminApi } from '../api/adminApi';
import { formatVND } from '../constants';

const { RangePicker } = DatePicker;

const RevenuePage = () => {
  const [loading, setLoading] = useState(true);
  const [dates, setDates] = useState([dayjs().subtract(7, 'day'), dayjs()]);
  const [summary, setSummary] = useState({
    totalRevenue: 0,
    totalBookings: 0,
    averageRevenue: 0,
  });
  const [reportData, setReportData] = useState([]);

  const loadData = async (dateRange) => {
    setLoading(true);
    const fromStr = dateRange[0].format('YYYY-MM-DD');
    const toStr = dateRange[1].format('YYYY-MM-DD');

    try {
      const { data } = await adminApi.getRevenue({ from: fromStr, to: toStr });
      const records = data.data || data;
      
      const list = Array.isArray(records) ? records : [];
      setReportData(list);

      // Compute summary stats
      const totalRev = list.reduce((sum, item) => sum + (item.revenue || 0), 0);
      const totalBks = list.reduce((sum, item) => sum + (item.bookingsCount || 0), 0);
      const avg = totalBks > 0 ? totalRev / totalBks : 0;

      setSummary({
        totalRevenue: totalRev,
        totalBookings: totalBks,
        averageRevenue: avg,
      });

      setLoading(false);
    } catch (error) {
      setLoading(false);
      message.error('Không thể tải báo cáo doanh thu');
    }
  };

  useEffect(() => {
    loadData(dates);
  }, []);

  const handleDateChange = (val) => {
    if (val && val[0] && val[1]) {
      setDates(val);
      loadData(val);
    }
  };

  const chartConfig = {
    data: reportData.map((item) => ({
      date: dayjs(item.date).format('DD/MM'),
      revenue: item.revenue || 0,
    })),
    xField: 'date',
    yField: 'revenue',
    label: {
      position: 'top',
      formatter: (v) => `${(v.revenue / 1000).toFixed(0)}k`,
      style: {
        fill: '#8c8c8c',
        fontSize: 10,
      },
    },
    meta: {
      date: { alias: 'Ngày' },
      revenue: { alias: 'Doanh thu (VND)' },
    },
  };

  const columns = [
    {
      title: 'Ngày',
      dataIndex: 'date',
      key: 'date',
      render: (text) => dayjs(text).format('DD/MM/YYYY'),
      sorter: (a, b) => dayjs(a.date).unix() - dayjs(b.date).unix(),
    },
    {
      title: 'Số lượt đặt chỗ',
      dataIndex: 'bookingsCount',
      key: 'bookingsCount',
      render: (text) => <strong>{text}</strong>,
      sorter: (a, b) => a.bookingsCount - b.bookingsCount,
    },
    {
      title: 'Doanh thu',
      dataIndex: 'revenue',
      key: 'revenue',
      render: (text) => formatVND(text),
      sorter: (a, b) => a.revenue - b.revenue,
    },
  ];

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      {/* Date Filter */}
      <Card className="glass-card" bordered={false}>
        <Space size="middle" wrap>
          <span style={{ fontWeight: 'bold' }}>Chọn khoảng thời gian:</span>
          <RangePicker
            value={dates}
            onChange={handleDateChange}
            allowClear={false}
            size="large"
            disabledDate={(current) => current && current > dayjs().endOf('day')}
          />
        </Space>
      </Card>

      {/* Summary Row */}
      <Row gutter={[16, 16]}>
        <Col xs={24} md={8}>
          <Card className="stat-card glass-card" bordered={false}>
            <Statistic
              title="Tổng doanh thu"
              value={summary.totalRevenue}
              formatter={(val) => formatVND(val)}
              valueStyle={{ color: '#008080', fontWeight: 'bold' }}
              prefix={<DollarOutlined />}
            />
          </Card>
        </Col>
        <Col xs={24} md={8}>
          <Card className="stat-card glass-card" bordered={false}>
            <Statistic
              title="Tổng lượt xe đặt"
              value={summary.totalBookings}
              valueStyle={{ color: '#1a237e' }}
              prefix={<TransactionOutlined />}
            />
          </Card>
        </Col>
        <Col xs={24} md={8}>
          <Card className="stat-card glass-card" bordered={false}>
            <Statistic
              title="Trung bình mỗi lượt đỗ"
              value={summary.averageRevenue}
              formatter={(val) => formatVND(val)}
              valueStyle={{ color: '#faad14' }}
              prefix={<CalendarOutlined />}
            />
          </Card>
        </Col>
      </Row>

      {/* Chart and Table */}
      {loading ? (
        <div style={{ textAlign: 'center', padding: '50px 0' }}>
          <Spin size="large" tip="Đang tải báo cáo..." />
        </div>
      ) : (
        <Row gutter={[16, 16]}>
          <Col xs={24} lg={14}>
            <Card title="Biểu đồ doanh thu hàng ngày" className="glass-card" bordered={false}>
              {reportData.length > 0 ? (
                <Column {...chartConfig} style={{ height: 320 }} />
              ) : (
                <div style={{ textAlign: 'center', padding: '50px 0', color: '#8c8c8c' }}>
                  Không có dữ liệu trong khoảng thời gian này
                </div>
              )}
            </Card>
          </Col>
          <Col xs={24} lg={10}>
            <Card title="Bảng số liệu chi tiết" className="glass-card" bordered={false}>
              <Table
                dataSource={reportData}
                columns={columns}
                rowKey="date"
                pagination={{ pageSize: 5 }}
                size="middle"
              />
            </Card>
          </Col>
        </Row>
      )}
    </Space>
  );
};

export default RevenuePage;
