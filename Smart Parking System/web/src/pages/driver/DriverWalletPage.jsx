import React, { useEffect, useState } from 'react';
import {
  Card, Row, Col, Statistic, Button, Table, Typography, Space, InputNumber,
  message, Empty, Spin, Tag,
} from 'antd';
import { WalletOutlined, PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import { walletApi } from '../../api/walletApi';
import dayjs from 'dayjs';

const { Text } = Typography;

const DriverWalletPage = () => {
  const [balance, setBalance] = useState(null);
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [topupAmount, setTopupAmount] = useState(50000);
  const [topupLoading, setTopupLoading] = useState(false);

  const fetchData = async () => {
    setLoading(true);
    try {
      const [balRes, txRes] = await Promise.all([
        walletApi.getBalance(),
        walletApi.getMyTransactions({ page: 0, size: 20 }),
      ]);
      setBalance(balRes.data?.data ?? balRes.data);
      const raw = txRes.data?.data ?? txRes.data;
      setTransactions(Array.isArray(raw) ? raw : (raw?.content ?? []));
    } catch (e) {
      message.error('Không thể tải thông tin ví');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const totalDeposited = transactions
    .filter(tx => tx.transactionRef?.startsWith('TOPUP') && tx.paymentStatus === 'COMPLETED')
    .reduce((acc, tx) => acc + Number(tx.amount), 0);

  const totalSpent = transactions
    .filter(tx => tx.transactionRef?.startsWith('CHECKOUT') && tx.paymentStatus === 'COMPLETED')
    .reduce((acc, tx) => acc + Number(tx.amount), 0);

  const handleTopup = async () => {
    if (!topupAmount || topupAmount < 10000) {
      message.warning('Số tiền tối thiểu là 10.000 ₫');
      return;
    }
    setTopupLoading(true);
    try {
      await walletApi.topup(topupAmount);
      message.success(`Nạp thành công ${topupAmount.toLocaleString('vi-VN')} ₫`);
      fetchData();
    } catch (e) {
      message.error(e.response?.data?.message || 'Nạp tiền thất bại');
    } finally {
      setTopupLoading(false);
    }
  };

  const quickAmounts = [50000, 100000, 200000, 500000];

  const columns = [
    {
      title: 'Mã GD',
      dataIndex: 'transactionRef',
      key: 'ref',
      render: (v) => v ? <Text code style={{ fontSize: 11 }}>{v}</Text> : '-',
    },
    {
      title: 'Loại',
      dataIndex: 'paymentMethod',
      key: 'type',
      render: (v) => <Tag>{v}</Tag>,
    },
    {
      title: 'Số tiền',
      dataIndex: 'amount',
      key: 'amount',
      render: (v, record) => {
        const isTopup = record.transactionRef?.startsWith('TOPUP');
        return (
          <Text strong style={{ color: isTopup ? '#52c41a' : '#ff4d4f' }}>
            {isTopup ? '+' : '-'}{Number(v).toLocaleString('vi-VN')} ₫
          </Text>
        );
      },
      align: 'right',
    },
    {
      title: 'Trạng thái',
      dataIndex: 'paymentStatus',
      key: 'status',
      render: (v) => (
        <Tag color={v === 'COMPLETED' ? 'green' : v === 'PENDING' ? 'gold' : 'red'}>
          {v === 'COMPLETED' ? 'Thành công' : v === 'PENDING' ? 'Đang xử lý' : 'Thất bại'}
        </Tag>
      ),
    },
    {
      title: 'Thời gian',
      dataIndex: 'createdAt',
      key: 'time',
      render: (v) => v ? dayjs(v).format('DD/MM/YYYY HH:mm') : '-',
      sorter: (a, b) => new Date(a.createdAt) - new Date(b.createdAt),
      defaultSortOrder: 'descend',
    },
  ];

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: 400 }}>
        <Spin size="large" />
      </div>
    );
  }

  return (
    <div>
      <Row gutter={[24, 24]}>
        {/* Balance card */}
        <Col xs={24} md={10}>
          <Card
            style={{
              borderRadius: 16,
              background: 'linear-gradient(135deg, #1a237e 0%, #0d47a1 100%)',
              border: 'none',
              color: '#fff',
              boxShadow: '0 8px 32px rgba(26, 35, 126, 0.3)',
            }}
          >
            <div style={{ marginBottom: 8 }}>
              <WalletOutlined style={{ fontSize: 32, color: 'rgba(255,255,255,0.8)' }} />
            </div>
            <div style={{ color: 'rgba(255,255,255,0.7)', fontSize: 14, marginBottom: 8 }}>
              Số dư khả dụng
            </div>
            <div style={{ fontSize: 36, fontWeight: 'bold', color: '#fff', marginBottom: 4 }}>
              {balance != null
                ? `${Number(balance).toLocaleString('vi-VN')} ₫`
                : '— ₫'}
            </div>
            
            <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 16, borderTop: '1px solid rgba(255,255,255,0.2)', paddingTop: 16 }}>
              <div>
                <div style={{ color: 'rgba(255,255,255,0.7)', fontSize: 12 }}>Đã nạp</div>
                <div style={{ color: '#52c41a', fontWeight: 'bold', fontSize: 16 }}>
                  +{totalDeposited.toLocaleString('vi-VN')} ₫
                </div>
              </div>
              <div style={{ textAlign: 'right' }}>
                <div style={{ color: 'rgba(255,255,255,0.7)', fontSize: 12 }}>Đã dùng</div>
                <div style={{ color: '#ff4d4f', fontWeight: 'bold', fontSize: 16 }}>
                  -{totalSpent.toLocaleString('vi-VN')} ₫
                </div>
              </div>
            </div>

            <Button
              icon={<ReloadOutlined />}
              onClick={fetchData}
              style={{
                marginTop: 16,
                background: 'rgba(255,255,255,0.15)',
                border: '1px solid rgba(255,255,255,0.3)',
                color: '#fff',
                width: '100%',
              }}
              size="small"
            >
              Làm mới
            </Button>
          </Card>

          {/* Top-up card */}
          <Card
            title="Nạp tiền vào ví"
            style={{ marginTop: 16, borderRadius: 12 }}
          >
            <Space wrap style={{ marginBottom: 16 }}>
              {quickAmounts.map((amt) => (
                <Button
                  key={amt}
                  type={topupAmount === amt ? 'primary' : 'default'}
                  onClick={() => setTopupAmount(amt)}
                  size="small"
                >
                  {amt.toLocaleString('vi-VN')} ₫
                </Button>
              ))}
            </Space>

            <Space.Compact style={{ width: '100%' }}>
              <InputNumber
                value={topupAmount}
                onChange={(v) => setTopupAmount(v)}
                min={10000}
                step={10000}
                formatter={(v) => `${v}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}
                parser={(v) => v.replace(/,/g, '')}
                style={{ width: '100%' }}
                addonAfter="₫"
              />
              <Button
                type="primary"
                icon={<PlusOutlined />}
                loading={topupLoading}
                onClick={handleTopup}
                style={{ background: '#52c41a', borderColor: '#52c41a' }}
              >
                Nạp tiền
              </Button>
            </Space.Compact>
          </Card>
        </Col>

        {/* Transaction history */}
        <Col xs={24} md={14}>
          <Card
            title="Lịch sử giao dịch"
            style={{ borderRadius: 12, boxShadow: '0 2px 8px rgba(0,0,0,0.06)' }}
          >
            {transactions.length === 0 ? (
              <Empty description="Chưa có giao dịch nào" />
            ) : (
              <Table
                dataSource={transactions}
                columns={columns}
                rowKey="id"
                pagination={{ pageSize: 8 }}
                size="small"
              />
            )}
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default DriverWalletPage;
