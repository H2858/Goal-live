-- SQL Script to set up GOAL LIVE database tables in Supabase SQL Editor.
-- This script creates the Leagues, Teams, Players, Matches, and Live_events tables.
-- It also sets up CASCADE deletes, default values, and configures Row Level Security (RLS)
-- with permissive policies so your Android app can easily read/write data using the anon key.

-- ==========================================
-- 1. DROP EXISTING TABLES (Optional / Clean Slate)
-- ==========================================
DROP TABLE IF EXISTS live_events CASCADE;
DROP TABLE IF EXISTS matches CASCADE;
DROP TABLE IF EXISTS players CASCADE;
DROP TABLE IF EXISTS teams CASCADE;
DROP TABLE IF EXISTS leagues CASCADE;
DROP TABLE IF EXISTS user_roles CASCADE;

-- ==========================================
-- 2. CREATE TABLES
-- ==========================================

-- Leagues Table
CREATE TABLE leagues (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    country TEXT NOT NULL,
    "logoUrl" TEXT NOT NULL, -- Keep casing in line with JSON / Android DTOs
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- User Roles Table
CREATE TABLE user_roles (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL,
    role TEXT NOT NULL DEFAULT 'user',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT unique_user_role UNIQUE (user_id, role)
);

-- Teams Table
CREATE TABLE teams (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    "leagueId" TEXT NOT NULL REFERENCES leagues(id) ON DELETE CASCADE,
    "logoUrl" TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Players Table
CREATE TABLE players (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    "teamId" TEXT NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    "photoUrl" TEXT NOT NULL,
    number INT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Matches Table
CREATE TABLE matches (
    id TEXT PRIMARY KEY,
    "homeTeamId" TEXT NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    "awayTeamId" TEXT NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    "leagueId" TEXT NOT NULL REFERENCES leagues(id) ON DELETE CASCADE,
    "homeScore" INT DEFAULT 0,
    "awayScore" INT DEFAULT 0,
    status TEXT DEFAULT 'LIVE', -- 'SCHEDULED', 'LIVE', 'FINISHED'
    minute INT DEFAULT 0,
    "startTime" BIGINT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Live Events Table
CREATE TABLE live_events (
    id TEXT PRIMARY KEY,
    "matchId" TEXT NOT NULL REFERENCES matches(id) ON DELETE CASCADE,
    type TEXT NOT NULL, -- 'GOAL', 'YELLOW_CARD', 'RED_CARD'
    minute INT NOT NULL,
    "scoringTeamId" TEXT NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    "scorerPlayerId" TEXT NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    "assistPlayerId" TEXT REFERENCES players(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ==========================================
-- 3. ENABLE ROW LEVEL SECURITY (RLS) & POLICIES
-- ==========================================
-- This makes sure the tables can be read and written by anyone using the public/anon key.

ALTER TABLE leagues ENABLE ROW LEVEL SECURITY;
ALTER TABLE teams ENABLE ROW LEVEL SECURITY;
ALTER TABLE players ENABLE ROW LEVEL SECURITY;
ALTER TABLE matches ENABLE ROW LEVEL SECURITY;
ALTER TABLE live_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_roles ENABLE ROW LEVEL SECURITY;

-- User Roles Policies
CREATE POLICY "Allow public read access to user_roles" ON user_roles FOR SELECT USING (true);
CREATE POLICY "Allow public insert/update/delete access to user_roles" ON user_roles FOR ALL USING (true);

-- Leagues Policies
CREATE POLICY "Allow public read access to leagues" ON leagues FOR SELECT USING (true);
CREATE POLICY "Allow public insert access to leagues" ON leagues FOR INSERT WITH CHECK (true);
CREATE POLICY "Allow public update access to leagues" ON leagues FOR UPDATE USING (true);
CREATE POLICY "Allow public delete access to leagues" ON leagues FOR DELETE USING (true);

-- Teams Policies
CREATE POLICY "Allow public read access to teams" ON teams FOR SELECT USING (true);
CREATE POLICY "Allow public insert access to teams" ON teams FOR INSERT WITH CHECK (true);
CREATE POLICY "Allow public update access to teams" ON teams FOR UPDATE USING (true);
CREATE POLICY "Allow public delete access to teams" ON teams FOR DELETE USING (true);

-- Players Policies
CREATE POLICY "Allow public read access to players" ON players FOR SELECT USING (true);
CREATE POLICY "Allow public insert access to players" ON players FOR INSERT WITH CHECK (true);
CREATE POLICY "Allow public update access to players" ON players FOR UPDATE USING (true);
CREATE POLICY "Allow public delete access to players" ON players FOR DELETE USING (true);

-- Matches Policies
CREATE POLICY "Allow public read access to matches" ON matches FOR SELECT USING (true);
CREATE POLICY "Allow public insert access to matches" ON matches FOR INSERT WITH CHECK (true);
CREATE POLICY "Allow public update access to matches" ON matches FOR UPDATE USING (true);
CREATE POLICY "Allow public delete access to matches" ON matches FOR DELETE USING (true);

-- Live Events Policies
CREATE POLICY "Allow public read access to live_events" ON live_events FOR SELECT USING (true);
CREATE POLICY "Allow public insert access to live_events" ON live_events FOR INSERT WITH CHECK (true);
CREATE POLICY "Allow public update access to live_events" ON live_events FOR UPDATE USING (true);
CREATE POLICY "Allow public delete access to live_events" ON live_events FOR DELETE USING (true);

-- ==========================================
-- 4. REALTIME ENABLEMENT (Optional but recommended)
-- ==========================================
-- Enables Realtime broadcast/replication on Matches and Live Events tables.
-- Run these statements to subscribe to realtime updates on matches and live_events.

ALTER PUBLICATION supabase_realtime ADD TABLE matches;
ALTER PUBLICATION supabase_realtime ADD TABLE live_events;

-- ==========================================
-- 5. INITIAL SEED DATA (To match the app's default beautiful setup)
-- ==========================================

-- Seed Leagues
INSERT INTO leagues (id, name, country, "logoUrl") VALUES
('L1', 'Champions League', 'Europe', 'ucl'),
('L2', 'Premier League', 'England', 'epl')
ON CONFLICT (id) DO NOTHING;

-- Seed Teams
INSERT INTO teams (id, name, "leagueId", "logoUrl") VALUES
('T1', 'Real Madrid', 'L1', 'real_madrid'),
('T2', 'Manchester City', 'L1', 'man_city'),
('T3', 'Bayern Munich', 'L1', 'bayern'),
('T4', 'Paris SG', 'L1', 'psg'),
('T5', 'Arsenal', 'L2', 'arsenal'),
('T6', 'Chelsea', 'L2', 'chelsea'),
('T7', 'Liverpool', 'L2', 'liverpool'),
('T8', 'Manchester Utd', 'L2', 'man_utd')
ON CONFLICT (id) DO NOTHING;

-- Seed Players
INSERT INTO players (id, name, "teamId", "photoUrl", number) VALUES
('P1', 'Kylian Mbappé', 'T1', 'mbappe', 9),
('P2', 'Vinicius Jr', 'T1', 'vinicius', 7),
('P3', 'Jude Bellingham', 'T1', 'bellingham', 5),
('P4', 'Erling Haaland', 'T2', 'haaland', 9),
('P4_2', 'Kevin De Bruyne', 'T2', 'debruyne', 17),
('P5', 'Harry Kane', 'T3', 'kane', 9),
('P6', 'Jamal Musiala', 'T3', 'musiala', 42),
('P7', 'Bukayo Saka', 'T5', 'saka', 7),
('P8', 'Martin Ødegaard', 'T5', 'odegaard', 8)
ON CONFLICT (id) DO NOTHING;

-- Seed a Default Live Match
INSERT INTO matches (id, "homeTeamId", "awayTeamId", "leagueId", "homeScore", "awayScore", status, minute, "startTime") VALUES
('m_default_1', 'T1', 'T2', 'L1', 0, 0, 'LIVE', 45, 1720618000000)
ON CONFLICT (id) DO NOTHING;

-- Seeding User Roles Example (Once you create a user in Supabase Auth, get their user UUID and insert here)
-- INSERT INTO user_roles (user_id, role) VALUES ('YOUR_USER_UUID_FROM_AUTH', 'admin') ON CONFLICT DO NOTHING;
