import { apiClient, publicApiClient, unwrap } from '../../lib/apiClient';
import type { ApiResponse, MemberCreateRequest, MemberResponse } from '../../types/api';

export function registerMember(request: MemberCreateRequest): Promise<MemberResponse> {
  return unwrap(publicApiClient.post<ApiResponse<MemberResponse>>('/members', request));
}

export function fetchMembers(): Promise<MemberResponse[]> {
  return unwrap(apiClient.get<ApiResponse<MemberResponse[]>>('/members'));
}

export function fetchMember(id: number): Promise<MemberResponse> {
  return unwrap(apiClient.get<ApiResponse<MemberResponse>>(`/members/${id}`));
}
