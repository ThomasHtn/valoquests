import { httpResource } from '@angular/common/http';
import { Service } from '@angular/core';

import { API_ENDPOINTS } from '@core/http/api-endpoints.constants';

import { Campaign } from './campaign.model';
import { CampaignHistory } from './campaign-history.model';
import { CampaignToday } from './campaign-today.model';

/**
 * Campaign resources, parameterless so every consumer shares the same request.
 */
@Service()
export class CampaignApi {
  /**
   * The campaign the site shows: the live one, else the last closed one, else nothing.
   */
  public readonly campaign = httpResource<Campaign>(() => API_ENDPOINTS.campaign);

  /**
   * What the squad brought in today, operator by operator.
   */
  public readonly today = httpResource<CampaignToday>(() => API_ENDPOINTS.campaignToday);

  /**
   * Every closed campaign, most recent first.
   */
  public readonly history = httpResource<readonly CampaignHistory[]>(
    () => API_ENDPOINTS.campaignHistory,
    { defaultValue: [] },
  );
}
